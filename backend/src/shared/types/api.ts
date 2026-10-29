export interface Topic {
  id: number;
  code: string;
  name: string;
  description: string | null;
  imageUrl: string | null;
  topicGroup?: 'SEMANTIC' | 'PART_OF_SPEECH';
}

export interface Flashcard {
  id: number;
  english: string;
  vietnamese: string;
  pronunciation: string | null;
  partOfSpeech: string | null;
  example: string | null;
  exampleVi: string | null;
  level: string | null;
  contentStatus: 'IMPORTED_UNVERIFIED' | 'SEMANTIC_VERIFIED';
}

export interface ApiErrorBody {
  error: { code: string; message: string };
}
