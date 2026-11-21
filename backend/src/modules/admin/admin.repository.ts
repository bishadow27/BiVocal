import type { Pool, PoolClient, QueryResultRow } from 'pg';
import { AppError } from '../../shared/errors/app-error.js';
import type { Pagination, TopicQuery, TopicInput, CardQuery, CardInput, CreateCardInput, UserQuery, Role, StatsQuery } from './admin.schema.js';
import type { AdminAccount, AdminUserRow, TopicRow, CardRow, PageRows, AdminProgressRow, AdminQuizRow,
  QuizAggregate, DayRow, TopicStatRow } from './admin.types.js';
import { createProgressRepository } from '../progress/progress.repository.js';
import { createQuizRepository } from '../quiz/quiz.repository.js';
import { createGamificationRepository } from '../gamification/gamification.repository.js';

const account = 'u.id, u.username, u.email, u.full_name, u.avatar_url, u.role, u.created_at, u.last_login';
const userColumns = `${account}, COALESCE(s.total_xp,0) AS total_xp,
  COALESCE(s.current_streak,0) AS current_streak, COALESCE(s.longest_streak,0) AS longest_streak`;
const topicColumns = `t.id, t.code, t.name, t.description, t.image_url,
  (SELECT COUNT(*)::text FROM flashcards f WHERE f.topic_id=t.id) AS flashcard_count`;
const cardColumns = `f.id, f.topic_id, t.code AS topic_code, t.name AS topic_name,
  f.english, f.vietnamese, f.pronunciation, f.part_of_speech, f.example, f.example_vi,
  f.level, f.is_active, f.created_at, f.updated_at`;
const userFrom = 'FROM users u LEFT JOIN user_scores s ON s.user_id=u.id';
const literalSearch = (input: string) => `%${input.replace(/[\\%_]/g, (char) => '\\' + char)}%`;
const rangeDays = { '7d': 7, '30d': 30, all: null } as const;
// Existing TIMESTAMP columns represent DB-local wall time. Convert using DB session
// timezone before UTC-day bucketing. Historical timezone configuration must be stable.
const quizUtc = "(q.answered_at AT TIME ZONE current_setting('TimeZone') AT TIME ZONE 'UTC')";
const statsFrom = `FROM quiz_results q JOIN flashcards f ON f.id=q.flashcard_id JOIN topics t ON t.id=f.topic_id
  WHERE ($1::integer IS NULL OR ${quizUtc} >= (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date - ($1 - 1))
    AND ($2::text IS NULL OR t.code=$2)`;
const aggregate = `COUNT(*)::text AS total_answers, COUNT(*) FILTER (WHERE q.is_correct)::text AS correct_answers,
  COUNT(*) FILTER (WHERE NOT q.is_correct)::text AS wrong_answers`;

const posTopics: Readonly<Record<string,string>> = Object.freeze({
  noun:'POS_NOUN',verb:'POS_VERB',adjective:'POS_ADJECTIVE',adverb:'POS_ADVERB',
  preposition:'POS_PREPOSITION',conjunction:'POS_CONJUNCTION',pronoun:'POS_PRONOUN',phrase:'POS_PHRASE',
});
function mappedPos(pos: CardInput['partOfSpeech']): string | null {
  if (pos === null) return null;
  if (!Object.hasOwn(posTopics,pos)) throw new AppError(400,'ADMIN_POS_INVALID','Unsupported part of speech');
  return posTopics[pos]!;
}

export function createAdminRepository(pool: Pool) {
  async function cardTransaction<T>(work: (client: PoolClient)=>Promise<T>): Promise<T> {
    const client=await pool.connect(); let discard=false;
    try { await client.query('BEGIN'); const value=await work(client); await client.query('COMMIT'); return value; }
    catch (error) { try { await client.query('ROLLBACK'); } catch { discard=true; } throw error; }
    finally { client.release(discard); }
  }
  async function cardTopics(client: PoolClient, primaryCode: string, posCode: string|null) {
    const rows=(await client.query<{id:number;code:string}>(
      'SELECT id,code FROM public.topics WHERE code=ANY($1::text[]) ORDER BY id FOR KEY SHARE',
      [[...new Set([primaryCode,...(posCode ? [posCode] : [])])]],
    )).rows;
    const primary=rows.find(t=>t.code===primaryCode);
    if (!primary) throw new AppError(404,'ADMIN_TOPIC_NOT_FOUND','Topic not found');
    if (primary.code.startsWith('POS_') && primary.code!==posCode)
      throw new AppError(409,'ADMIN_PRIMARY_POS_CONFLICT','Primary POS topic conflicts with part of speech');
    const pos=posCode ? rows.find(t=>t.code===posCode) : undefined;
    if (posCode && !pos) throw new AppError(409,'ADMIN_POS_TOPIC_MISSING','Mapped POS topic is unavailable');
    return {primary,pos};
  }
  async function savedCard(client: PoolClient,id: number) {
    const row=(await client.query<CardRow>(
      `SELECT ${cardColumns} FROM public.flashcards f JOIN public.topics t ON t.id=f.topic_id WHERE f.id=$1`,[id])).rows[0];
    if (!row) throw new AppError(409,'ADMIN_TOPIC_MEMBERSHIP_DRIFT','Card disappeared during transaction');
    return row;
  }
  async function addPos(client: PoolClient,cardId: number,primaryId: number,pos: {id:number}|undefined) {
    if (pos && pos.id!==primaryId) await client.query(
      'INSERT INTO public.flashcard_topics(flashcard_id,topic_id,is_primary) VALUES($1,$2,FALSE) ON CONFLICT (flashcard_id,topic_id) DO NOTHING',
      [cardId,pos.id]);
  }

  async function page<T extends QueryResultRow>(columns: string, from: string, order: string,
    params: unknown[], pagination: Pagination): Promise<PageRows<T>> {
    // SQL fragments are private constants, never supplied by callers/requests.
    const [count, items] = await Promise.all([
      pool.query<{ total: string }>(`SELECT COUNT(*)::text AS total ${from}`, params),
      pool.query<T>(`SELECT ${columns} ${from} ORDER BY ${order} LIMIT $${params.length + 1} OFFSET $${params.length + 2}`,
        [...params, pagination.limit, (pagination.page - 1) * pagination.limit]),
    ]);
    return { rows: items.rows, total: count.rows[0]!.total };
  }
  async function stats(query: StatsQuery) {
    const params = [rangeDays[query.range], query.topicCode ?? null];
    const [totals, topics, days] = await Promise.all([
      pool.query<QuizAggregate>(`SELECT ${aggregate} ${statsFrom}`, params),
      pool.query<TopicStatRow>(`SELECT t.code,t.name,${aggregate} ${statsFrom}
        GROUP BY t.id,t.code,t.name ORDER BY COUNT(*) DESC,t.code ASC`, params),
      pool.query<DayRow>(`WITH daily AS (
        SELECT ${quizUtc}::date AS day,COUNT(*) AS answers,
          COUNT(*) FILTER (WHERE q.is_correct) AS correct,COUNT(*) FILTER (WHERE NOT q.is_correct) AS wrong
        ${statsFrom} GROUP BY ${quizUtc}::date
      ), calendar AS (
        SELECT day FROM daily WHERE $1::integer IS NULL
        UNION ALL
        SELECT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date - n FROM generate_series(0,$1::integer-1) AS n
      ) SELECT to_char(c.day,'YYYY-MM-DD') AS date,COALESCE(d.answers,0)::text AS answers,
          COALESCE(d.correct,0)::text AS correct,COALESCE(d.wrong,0)::text AS wrong
        FROM calendar c LEFT JOIN daily d ON d.day=c.day ORDER BY c.day`, params),
    ]);
    return { totals: totals.rows[0]!, byTopic: topics.rows, byDay: days.rows };
  }
  return {
    async account(id: number) {
      return (await pool.query<AdminAccount>(`SELECT ${account} FROM users u WHERE u.id=$1`, [id])).rows[0];
    },
    async setRole(id: number, role: Role) {
      return (await pool.query<AdminAccount>(`UPDATE users AS u SET role=$2 WHERE u.id=$1 RETURNING ${account}`, [id, role])).rows[0];
    },
    async setRoleByEmail(email: string, role: Role) {
      return (await pool.query<{ id: number; role: Role }>(
        'UPDATE users SET role=$2 WHERE LOWER(email)=$1 RETURNING id,role', [email, role])).rows[0];
    },
    async dashboard(range: StatsQuery['range']) {
      const [counts, quiz, topUsers] = await Promise.all([
        pool.query<{ users: string; cards: string; active: string; inactive: string; xp: string }>(
          `SELECT (SELECT COUNT(*)::text FROM users) AS users,
            (SELECT COUNT(*)::text FROM flashcards) AS cards,
            (SELECT COUNT(*)::text FROM flashcards WHERE is_active IS TRUE) AS active,
            (SELECT COUNT(*)::text FROM flashcards WHERE is_active IS NOT TRUE) AS inactive,
            (SELECT COALESCE(SUM(total_xp),0)::text FROM user_scores) AS xp`),
        stats({ range }), createGamificationRepository(pool).leaderboard(5),
      ]);
      return { counts: counts.rows[0]!, quiz, topUsers };
    },
    stats,
    async topics(query: TopicQuery) {
      return page<TopicRow>(topicColumns, "FROM topics t WHERE (t.code ILIKE $1 ESCAPE '\\' OR t.name ILIKE $1 ESCAPE '\\')",
        't.id ASC', [literalSearch(query.search)], query);
    },
    async topicByCode(code: string) {
      return (await pool.query<{ id: number }>('SELECT id FROM topics WHERE code=$1', [code])).rows[0];
    },
    async createTopic(input: TopicInput) {
      return (await pool.query<TopicRow>(`WITH saved AS (
        INSERT INTO topics(code,name,description,image_url) VALUES($1,$2,$3,$4)
        RETURNING id,code,name,description,image_url)
        SELECT ${topicColumns} FROM saved t`, [input.code, input.name, input.description, input.imageUrl])).rows[0]!;
    },
    async updateTopic(id: number, input: TopicInput) {
      return (await pool.query<TopicRow>(`WITH saved AS (
        UPDATE topics SET code=$2,name=$3,description=$4,image_url=$5,updated_at=CURRENT_TIMESTAMP WHERE id=$1
        RETURNING id,code,name,description,image_url)
        SELECT ${topicColumns} FROM saved t`, [id, input.code, input.name, input.description, input.imageUrl])).rows[0];
    },
    async cards(query: CardQuery) {
      return page<CardRow>(cardColumns, `FROM flashcards f JOIN topics t ON t.id=f.topic_id
        WHERE (f.english ILIKE $1 ESCAPE '\\' OR f.vietnamese ILIKE $1 ESCAPE '\\')
          AND ($2::text IS NULL OR t.code=$2)
          AND ($3='ALL' OR ($3='ACTIVE' AND f.is_active IS TRUE) OR ($3='INACTIVE' AND f.is_active IS NOT TRUE))
          AND ($4::text IS NULL OR f.level=$4)`, 'f.id ASC',
      [literalSearch(query.search), query.topicCode ?? null, query.status, query.level ?? null], query);
    },
    async createCard(input: CreateCardInput): Promise<CardRow | undefined> {
      const posCode=mappedPos(input.partOfSpeech);
      return cardTransaction(async client=>{
        const {primary,pos}=await cardTopics(client,input.topicCode,posCode);
        const card=(await client.query<{id:number}>(
          `INSERT INTO public.flashcards(topic_id,english,vietnamese,pronunciation,part_of_speech,example,example_vi,level,is_active)
           VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9) RETURNING id`,
          [primary.id,input.english,input.vietnamese,input.pronunciation,input.partOfSpeech,input.example,input.exampleVi,input.level,input.isActive])).rows[0]!;
        await client.query('INSERT INTO public.flashcard_topics(flashcard_id,topic_id,is_primary) VALUES($1,$2,TRUE)',[card.id,primary.id]);
        await addPos(client,card.id,primary.id,pos);
        return savedCard(client,card.id);
      });
    },
    async updateCard(id: number, input: CardInput) {
      const posCode=mappedPos(input.partOfSpeech);
      return cardTransaction(async client=>{
        // Card lock serializes cooperating writers, including concurrent Admin requests.
        const card=(await client.query<{id:number;topic_id:number;part_of_speech:string|null}>(
          'SELECT id,topic_id,part_of_speech FROM public.flashcards WHERE id=$1 FOR UPDATE',[id])).rows[0];
        if (!card) return undefined;
        const links=(await client.query<{topic_id:number;is_primary:boolean;code:string}>(
          `SELECT ft.topic_id,ft.is_primary,t.code FROM public.flashcard_topics ft
           JOIN public.topics t ON t.id=ft.topic_id WHERE ft.flashcard_id=$1
           ORDER BY ft.topic_id FOR UPDATE OF ft`,[id])).rows;
        const primaries=links.filter(l=>l.is_primary);
        if (new Set(links.map(l=>l.topic_id)).size!==links.length || primaries.length!==1 || primaries[0]!.topic_id!==card.topic_id)
          throw new AppError(409,'ADMIN_TOPIC_MEMBERSHIP_DRIFT','Primary topic membership is inconsistent; refresh and review');
        const {primary,pos}=await cardTopics(client,input.topicCode,posCode);
        await client.query(
          `UPDATE public.flashcards SET topic_id=$2,english=$3,vietnamese=$4,pronunciation=$5,part_of_speech=$6,
           example=$7,example_vi=$8,level=$9,updated_at=CURRENT_TIMESTAMP WHERE id=$1`,
          [id,primary.id,input.english,input.vietnamese,input.pronunciation,input.partOfSpeech,input.example,input.exampleVi,input.level]);
        // A2: remove A first, then promote B; never demote A into a secondary.
        if (card.topic_id!==primary.id) await client.query(
          'DELETE FROM public.flashcard_topics WHERE flashcard_id=$1 AND topic_id=$2',[id,card.topic_id]);
        await client.query(
          'INSERT INTO public.flashcard_topics(flashcard_id,topic_id,is_primary) VALUES($1,$2,TRUE) ON CONFLICT (flashcard_id,topic_id) DO UPDATE SET is_primary=TRUE',[id,primary.id]);
        if (card.part_of_speech!==input.partOfSpeech) {
          await client.query(
            `DELETE FROM public.flashcard_topics ft USING public.topics t
             WHERE ft.flashcard_id=$1 AND ft.topic_id=t.id AND NOT ft.is_primary
               AND left(t.code,4)='POS_' AND ($2::text IS NULL OR t.code<>$2)`,[id,posCode]);
          await addPos(client,id,primary.id,pos);
        }
        return savedCard(client,id);
      });
    },
    async setCardStatus(id: number, isActive: boolean) {
      return (await pool.query<CardRow>(`WITH saved AS (
        UPDATE flashcards SET is_active=$2,updated_at=CURRENT_TIMESTAMP WHERE id=$1
        RETURNING id,topic_id,english,vietnamese,pronunciation,part_of_speech,example,example_vi,level,is_active,created_at,updated_at)
        SELECT ${cardColumns} FROM saved f JOIN topics t ON t.id=f.topic_id`, [id,isActive])).rows[0];
    },
    async users(query: UserQuery) {
      return page<AdminUserRow>(userColumns, `${userFrom}
        WHERE (u.username ILIKE $1 ESCAPE '\\' OR u.email ILIKE $1 ESCAPE '\\' OR u.full_name ILIKE $1 ESCAPE '\\')
          AND ($2='ALL' OR u.role=$2)`, 'u.id ASC', [literalSearch(query.search),query.role], query);
    },
    async user(id: number) {
      return (await pool.query<AdminUserRow>(`SELECT ${userColumns} ${userFrom} WHERE u.id=$1`, [id])).rows[0];
    },
    progressSummary: createProgressRepository(pool).summaryByUser,
    quizSummary: createQuizRepository(pool).summary,
    score: createGamificationRepository(pool).findScore,
    async progress(id: number, query: TopicQuery) {
      // Admin inspection deliberately includes inactive cards and preserves old history.
      return page<AdminProgressRow>(`p.flashcard_id,f.english,f.vietnamese,t.code AS topic_code,p.memory_level,
        p.review_count,p.correct_count,p.wrong_count,p.is_favorite,p.last_review,p.next_review`,
      `FROM user_progress p JOIN flashcards f ON f.id=p.flashcard_id JOIN topics t ON t.id=f.topic_id
       WHERE p.user_id=$1 AND (f.english ILIKE $2 ESCAPE '\\' OR f.vietnamese ILIKE $2 ESCAPE '\\')`,
      'p.updated_at DESC,p.flashcard_id ASC', [id,literalSearch(query.search)], query);
    },
    async quizHistory(id: number, query: Pagination) {
      return page<AdminQuizRow>(`q.id,q.flashcard_id,f.english AS question,selected.vietnamese AS selected_answer,
        f.vietnamese AS correct_answer,q.is_correct,q.answered_at`,
      `FROM quiz_results q JOIN flashcards f ON f.id=q.flashcard_id
        LEFT JOIN flashcards selected ON selected.id=q.selected_flashcard_id WHERE q.user_id=$1`,
      'q.answered_at DESC,q.id DESC', [id], query);
    },
    async leaderboard(query: Pagination) {
      return page<import('../gamification/gamification.types.js').LeaderboardRow>(
        `(RANK() OVER (ORDER BY s.total_xp DESC))::text AS rank,s.user_id,u.username,u.full_name,u.avatar_url,
          s.total_xp,s.current_streak,s.longest_streak`,
        'FROM user_scores s JOIN users u ON u.id=s.user_id WHERE s.total_xp>0',
        's.total_xp DESC,s.user_id ASC', [], query);
    },
  };
}
export type AdminRepository = ReturnType<typeof createAdminRepository>;
