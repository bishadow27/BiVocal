import { AppError } from '../../shared/errors/app-error.js';
import type { AdminRepository } from './admin.repository.js';
import type { AdminAccount, AdminUserRow, CardRow, TopicRow, PageRows, QuizAggregate } from './admin.types.js';
import type { TopicInput, TopicQuery, CardInput, CreateCardInput, CardQuery, UserQuery, Pagination, Role, StatsQuery } from './admin.schema.js';
import { levelForXp } from '../gamification/gamification.service.js';

export function adminCount(value: string): number {
  const count = Number(value);
  if (!Number.isSafeInteger(count) || count < 0) throw new Error('Invalid admin aggregate');
  return count;
}
const accuracy = (correct: number, total: number) => total === 0 ? null : Math.round(correct / total * 10000) / 100;
const iso = (date: Date | null) => date?.toISOString() ?? null;
export const toAdminMe = (u: AdminAccount) => ({ id:u.id,username:u.username,email:u.email,
  fullName:u.full_name,avatarUrl:u.avatar_url,role:u.role });
const toUser = (u: AdminUserRow) => ({ ...toAdminMe(u),createdAt:iso(u.created_at),lastLogin:iso(u.last_login),
  totalXp:u.total_xp,level:levelForXp(u.total_xp),currentStreak:u.current_streak,longestStreak:u.longest_streak });
const toTopic = (t: TopicRow) => ({ id:t.id,code:t.code,name:t.name,description:t.description,imageUrl:t.image_url,
  flashcardCount:adminCount(t.flashcard_count) });
const toCard = (f: CardRow) => ({ id:f.id,topicId:f.topic_id,topicCode:f.topic_code,topicName:f.topic_name,
  english:f.english,vietnamese:f.vietnamese,pronunciation:f.pronunciation,partOfSpeech:f.part_of_speech,
  example:f.example,exampleVi:f.example_vi,level:f.level,isActive:f.is_active === true,
  createdAt:iso(f.created_at),updatedAt:iso(f.updated_at) });
function paged<T,U>(data: PageRows<T>, query: Pagination, map: (row: T)=>U) {
  const total=adminCount(data.total);
  return { items:data.rows.map(map),page:query.page,limit:query.limit,total,totalPages:Math.ceil(total/query.limit) };
}
function quizTotals(row: QuizAggregate) {
  const totalAnswers=adminCount(row.total_answers),correctAnswers=adminCount(row.correct_answers);
  return { totalAnswers,correctAnswers,wrongAnswers:adminCount(row.wrong_answers),accuracy:accuracy(correctAnswers,totalAnswers) };
}
function mapStats(result: Awaited<ReturnType<AdminRepository['stats']>>) {
  return { ...quizTotals(result.totals),byDay:result.byDay.map((d)=>({date:d.date,answers:adminCount(d.answers),
    correct:adminCount(d.correct),wrong:adminCount(d.wrong)})),
  byTopic:result.byTopic.map((t)=>({code:t.code,name:t.name,...quizTotals(t)})) };
}
function missing(kind: 'TOPIC'|'FLASHCARD'|'USER'): never {
  throw new AppError(404,`ADMIN_${kind}_NOT_FOUND`,`${kind === 'FLASHCARD' ? 'Flashcard' : kind === 'TOPIC' ? 'Topic' : 'User'} not found`);
}
async function mutation<T>(operation: ()=>Promise<T>): Promise<T> {
  try { return await operation(); } catch (error: unknown) {
    if (typeof error === 'object' && error !== null && 'code' in error && error.code === '23505' && 'constraint' in error) {
      if (error.constraint === 'topics_code_key') throw new AppError(409,'TOPIC_CODE_EXISTS','Topic code already exists');
      if (error.constraint === 'topics_name_key') throw new AppError(409,'TOPIC_NAME_EXISTS','Topic name already exists');
      if (error.constraint === 'idx_flashcards_unique_word_meaning') throw new AppError(409,'FLASHCARD_EXISTS','Flashcard word and meaning already exist');
    }
    if (typeof error === 'object' && error !== null && 'code' in error && error.code === '23503') {
      throw new AppError(409,'ADMIN_REFERENCE_CONFLICT','Referenced data changed; refresh and retry');
    }
    throw error;
  }
}
export function createAdminService(repository: AdminRepository) {
  async function ensureUser(id: number) { const row=await repository.user(id); if (!row) missing('USER'); return row; }
  async function ensureTopic(code: string) { if (!await repository.topicByCode(code)) missing('TOPIC'); }
  return {
    async authorize(id: number) {
      const user=await repository.account(id);
      if (!user) throw new AppError(401,'AUTH_USER_NOT_FOUND','Authenticated user not found');
      if (user.role !== 'ADMIN') throw new AppError(403,'ADMIN_FORBIDDEN','Administrator access required');
      return toAdminMe(user);
    },
    async dashboard(range: StatsQuery['range']) {
      const data=await repository.dashboard(range); const quiz=mapStats(data.quiz);
      return { users:{total:adminCount(data.counts.users)},flashcards:{total:adminCount(data.counts.cards),
        active:adminCount(data.counts.active),inactive:adminCount(data.counts.inactive)},
      quiz:quizTotals(data.quiz.totals),gamification:{totalXp:adminCount(data.counts.xp)},
      topUsers:data.topUsers.map((u)=>({userId:u.user_id,username:u.username,fullName:u.full_name,
        totalXp:u.total_xp,level:levelForXp(u.total_xp),currentStreak:u.current_streak})),
      topTopics:data.quiz.byTopic.slice(0,5).map((t)=>({code:t.code,name:t.name,quizCount:adminCount(t.total_answers)})),
      quizByDay:quiz.byDay };
    },
    async topics(query: TopicQuery) { return paged(await repository.topics(query),query,toTopic); },
    async createTopic(input: TopicInput) { return toTopic(await mutation(()=>repository.createTopic(input))); },
    async updateTopic(id: number,input: TopicInput) {
      const row=await mutation(()=>repository.updateTopic(id,input)); if (!row) missing('TOPIC'); return toTopic(row);
    },
    async cards(query: CardQuery) { return paged(await repository.cards(query),query,toCard); },
    async createCard(input: CreateCardInput) {
      await ensureTopic(input.topicCode); const row=await mutation(()=>repository.createCard(input));
      if (!row) missing('TOPIC'); return toCard(row);
    },
    async updateCard(id: number,input: CardInput) {
      await ensureTopic(input.topicCode); const row=await mutation(()=>repository.updateCard(id,input));
      if (!row) missing('FLASHCARD'); return toCard(row);
    },
    async status(id: number,isActive: boolean) {
      const row=await repository.setCardStatus(id,isActive); if (!row) missing('FLASHCARD'); return toCard(row);
    },
    async users(query: UserQuery) { return paged(await repository.users(query),query,toUser); },
    async user(id: number) {
      const user=await ensureUser(id);
      const [p,q,s]=await Promise.all([repository.progressSummary(id),repository.quizSummary(id),repository.score(id)]);
      const totalReviews=adminCount(p.total_reviews),correctAnswers=adminCount(p.correct_answers);
      const xp=s?.total_xp ?? 0;
      return { user:toUser(user),progress:{trackedCards:adminCount(p.tracked_cards),reviewedCards:adminCount(p.reviewed_cards),
        favoriteCards:adminCount(p.favorite_cards),masteredCards:adminCount(p.mastered_cards),totalReviews,correctAnswers,
        wrongAnswers:adminCount(p.wrong_answers),accuracy:accuracy(correctAnswers,totalReviews)},
      quiz:quizTotals(q),gamification:{totalXp:xp,level:levelForXp(xp),xpIntoLevel:xp%100,xpPerLevel:100,
        currentStreak:s?.current_streak ?? 0,longestStreak:s?.longest_streak ?? 0,lastActivityDate:s?.last_activity_date ?? null} };
    },
    async progress(id: number,query: TopicQuery) {
      await ensureUser(id);
      return paged(await repository.progress(id,query),query,(p)=>({flashcardId:p.flashcard_id,english:p.english,
        vietnamese:p.vietnamese,topicCode:p.topic_code,memoryLevel:p.memory_level,reviewCount:p.review_count,
        correctCount:p.correct_count,wrongCount:p.wrong_count,isFavorite:p.is_favorite,
        lastReview:iso(p.last_review),nextReview:iso(p.next_review)}));
    },
    async quizHistory(id: number,query: Pagination) {
      await ensureUser(id);
      return paged(await repository.quizHistory(id,query),query,(q)=>({id:q.id,flashcardId:q.flashcard_id,question:q.question,
        selectedAnswer:q.selected_answer,correctAnswer:q.correct_answer,correct:q.is_correct,answeredAt:iso(q.answered_at)}));
    },
    async setRole(actor: number,id: number,role: Role) {
      if (actor === id && role === 'USER') throw new AppError(409,'ADMIN_SELF_ROLE_CHANGE_FORBIDDEN','Cannot demote your own account');
      const row=await repository.setRole(id,role); if (!row) missing('USER'); return toAdminMe(row);
    },
    async stats(query: StatsQuery) {
      if (query.topicCode !== undefined) await ensureTopic(query.topicCode);
      return mapStats(await repository.stats(query));
    },
    async leaderboard(query: Pagination) {
      return paged(await repository.leaderboard(query),query,(u)=>({rank:adminCount(u.rank),userId:u.user_id,
        username:u.username,fullName:u.full_name,avatarUrl:u.avatar_url,totalXp:u.total_xp,level:levelForXp(u.total_xp),
        currentStreak:u.current_streak,longestStreak:u.longest_streak}));
    },
  };
}
export type AdminService = ReturnType<typeof createAdminService>;
