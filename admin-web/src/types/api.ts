export type Role = 'USER' | 'ADMIN';
export type Range = '7d' | '30d' | 'all';
export type Level = 'A1' | 'A2' | 'B1' | 'B2';
export type PartOfSpeech =
  'noun' | 'verb' | 'adjective' | 'adverb' | 'preposition' | 'conjunction' | 'pronoun' | 'phrase';
export interface PagedResponse<T> {
  items: T[];
  page: number;
  limit: number;
  total: number;
  totalPages: number;
}
export interface Account {
  id: number;
  username: string;
  email: string;
  fullName: string | null;
  avatarUrl: string | null;
}
export interface AdminMe extends Account {
  role: 'ADMIN';
}
export interface LoginResponse {
  user: Account;
  accessToken: string;
}
export interface QuizSummary {
  totalAnswers: number;
  correctAnswers: number;
  wrongAnswers: number;
  accuracy: number | null;
}
export interface QuizDay {
  date: string;
  answers: number;
  correct: number;
  wrong: number;
}
export interface QuizTopic extends QuizSummary {
  code: string;
  name: string;
}
export interface QuizStats extends QuizSummary {
  byDay: QuizDay[];
  byTopic: QuizTopic[];
}
export interface AdminDashboard {
  users: { total: number };
  flashcards: { total: number; active: number; inactive: number };
  quiz: QuizSummary;
  gamification: { totalXp: number };
  quizByDay: QuizDay[];
  topUsers: {
    userId: number;
    username: string;
    fullName: string | null;
    totalXp: number;
    level: number;
    currentStreak: number;
  }[];
  topTopics: { code: string; name: string; quizCount: number }[];
}
export interface AdminTopic {
  id: number;
  code: string;
  name: string;
  description: string | null;
  imageUrl: string | null;
  flashcardCount: number;
}
export interface TopicInput {
  code: string;
  name: string;
  description: string | null;
  imageUrl: string | null;
}
export interface CardInput {
  topicCode: string;
  english: string;
  vietnamese: string;
  pronunciation: string | null;
  partOfSpeech: PartOfSpeech | null;
  example: string | null;
  exampleVi: string | null;
  level: Level;
}
export interface CreateCardInput extends CardInput {
  isActive: boolean;
}
export interface AdminFlashcard {
  id: number;
  topicId: number;
  topicCode: string;
  topicName: string;
  english: string;
  vietnamese: string;
  pronunciation: string | null;
  partOfSpeech: string | null;
  example: string | null;
  exampleVi: string | null;
  level: string | null;
  isActive: boolean;
  createdAt: string | null;
  updatedAt: string | null;
}
export interface AdminUser extends Account {
  role: Role;
  createdAt: string | null;
  lastLogin: string | null;
  totalXp: number;
  level: number;
  currentStreak: number;
  longestStreak: number;
}
export interface ProgressSummary {
  trackedCards: number;
  reviewedCards: number;
  favoriteCards: number;
  masteredCards: number;
  totalReviews: number;
  correctAnswers: number;
  wrongAnswers: number;
  accuracy: number | null;
}
export interface GamificationSummary {
  totalXp: number;
  level: number;
  xpIntoLevel: number;
  xpPerLevel: number;
  currentStreak: number;
  longestStreak: number;
  lastActivityDate: string | null;
}
export interface UserDetail {
  user: AdminUser;
  progress: ProgressSummary;
  quiz: QuizSummary;
  gamification: GamificationSummary;
}
export interface AdminProgress {
  flashcardId: number;
  english: string;
  vietnamese: string;
  topicCode: string;
  memoryLevel: number;
  reviewCount: number;
  correctCount: number;
  wrongCount: number;
  isFavorite: boolean;
  lastReview: string | null;
  nextReview: string | null;
}
export interface AdminQuizHistory {
  id: number;
  flashcardId: number;
  question: string;
  selectedAnswer: string | null;
  correctAnswer: string;
  correct: boolean;
  answeredAt: string;
}
export interface LeaderboardItem {
  rank: number;
  userId: number;
  username: string;
  fullName: string | null;
  avatarUrl: string | null;
  totalXp: number;
  level: number;
  currentStreak: number;
  longestStreak: number;
}
export interface Pagination {
  page: number;
  limit: number;
}
export interface TopicFilters extends Pagination {
  search: string;
}
export interface FlashcardFilters extends TopicFilters {
  topicCode: string;
  status: 'ALL' | 'ACTIVE' | 'INACTIVE';
  level: '' | Level;
}
export interface UserFilters extends TopicFilters {
  role: 'ALL' | Role;
}
export type ReviewStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'PUBLISHED';
export interface VocabularyReviewCandidate {
  candidateId:string; english:string; vietnamese:string; pronunciation:string|null; partOfSpeech:string|null;
  cefr:Level|null; primaryTopicCode:string|null; topicCodes:string[]; example:string|null; exampleVi:string|null;
  source:{code:string;name:string;url:string;version:string;license:string;recordId:string};
  validationReasons:string[]; qualityScore:number; qualityVerified:boolean;
  licenseStatus:'ATTRIBUTION_REQUIRED'|'VERIFIED'|'BLOCKED'; status:ReviewStatus;
  reviewer:string|null; reviewedAt:string|null; publishedFlashcardId:number|null; rowVersion:number; updatedAt:string;
  duplicate:{type:'NONE'|'EXACT'|'SAME_WORD_DIFFERENT_SENSE';flashcardId:number|null;sameWordCount:number}; multiSense:boolean;
}
export interface VocabularyReviewStats {total:number;pending:number;approved:number;rejected:number;published:number}
export interface VocabularyReviewFilters extends Pagination {search:string;level:string;topicCode:string;partOfSpeech:string;status:string;reason:string;source:string}
export interface VocabularyReviewOptions {topics:string[];parts:string[];reasons:string[];sources:{value:string;label:string}[]}
export interface VocabularyReviewEdit {english:string;vietnamese:string;pronunciation:string|null;partOfSpeech:string|null;cefr:Level;primaryTopicCode:string;topicCodes:string[];example:string;exampleVi:string|null;rowVersion:number}
export interface VocabularyPublishPreview {batchId:string;status:string;total:number;toInsert:number;toSkip:number;blocked:number;items:{id:string;version:number;disposition:string;duplicateId:number|null;reason:string|null}[]}
