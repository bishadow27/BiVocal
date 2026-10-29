import type { Pool } from 'pg';

export interface FlashcardRow {
  id: number;
  english: string;
  vietnamese: string;
  pronunciation: string | null;
  part_of_speech: string | null;
  example: string | null;
  example_vi: string | null;
  level: string | null;
  content_status?: 'IMPORTED_UNVERIFIED' | 'SEMANTIC_VERIFIED';
}

export interface FlashcardRepository {
  findByTopicCode(code: string): Promise<FlashcardRow[]>;
  search(query: FlashcardSearchQuery): Promise<{ rows: FlashcardRow[]; total: number }>;
  findById(id: number): Promise<FlashcardRow | undefined>;
}

export interface FlashcardSearchQuery {
  search: string | null;
  topicCode: string | null;
  level: string | null;
  page: number;
  limit: number;
}

function literalSearch(value: string | null): string | null {
  return value === null ? null : `%${value.replace(/[\\%_]/g, match => `\\${match}`)}%`;
}

export function createFlashcardRepository(pool: Pool): FlashcardRepository {
  const contentStatus = `CASE WHEN EXISTS (SELECT 1 FROM flashcard_provenance status_provenance
    WHERE status_provenance.flashcard_id=f.id
      AND status_provenance.validation_status='NEEDS_REVIEW'
      AND status_provenance.validation_reasons @> '["IMPORTED_UNVERIFIED"]'::jsonb)
    THEN 'IMPORTED_UNVERIFIED' ELSE 'SEMANTIC_VERIFIED' END AS content_status`;
  return {
    async findByTopicCode(code) {
      const result = await pool.query<FlashcardRow>(
        `SELECT f.id, f.english, f.vietnamese, f.pronunciation,
                f.part_of_speech, f.example, f.example_vi, f.level, ${contentStatus}
         FROM flashcards f
         JOIN flashcard_topics ft ON ft.flashcard_id = f.id
         JOIN topics t ON t.id = ft.topic_id
         WHERE t.code = $1 AND f.is_active = TRUE
         ORDER BY f.id`,
        [code],
      );
      return result.rows;
    },
    async search(query) {
      const values = [literalSearch(query.search), query.topicCode, query.level,
        query.limit, (query.page - 1) * query.limit];
      const where = `f.is_active=TRUE
        AND ($1::text IS NULL OR f.english ILIKE $1 ESCAPE '\\' OR f.vietnamese ILIKE $1 ESCAPE '\\')
        AND ($2::text IS NULL OR EXISTS (SELECT 1 FROM flashcard_topics ft
          JOIN topics filter_topic ON filter_topic.id=ft.topic_id
          WHERE ft.flashcard_id=f.id AND filter_topic.code=$2))
        AND ($3::text IS NULL OR f.level=$3)`;
      const [items, count] = await Promise.all([
        pool.query<FlashcardRow>(`SELECT f.id,f.english,f.vietnamese,f.pronunciation,
          f.part_of_speech,f.example,f.example_vi,f.level,${contentStatus} FROM flashcards f
          WHERE ${where} ORDER BY LOWER(f.english),f.id LIMIT $4 OFFSET $5`, values),
        pool.query<{ total: string }>(`SELECT COUNT(*)::text AS total FROM flashcards f
          WHERE ${where}`, values.slice(0, 3)),
      ]);
      return { rows: items.rows, total: Number(count.rows[0]?.total ?? 0) };
    },
    async findById(id) {
      const result = await pool.query<FlashcardRow>(
        `SELECT f.id, f.english, f.vietnamese, f.pronunciation,
                f.part_of_speech, f.example, f.example_vi, f.level, ${contentStatus}
         FROM flashcards f
         WHERE f.id = $1 AND f.is_active = TRUE`,
        [id],
      );
      return result.rows[0];
    },
  };
}
