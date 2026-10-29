import type { Pool } from 'pg';

export interface TopicRow {
  id: number;
  code: string;
  name: string;
  description: string | null;
  image_url: string | null;
}

export interface TopicCatalogRow extends TopicRow { flashcard_count: string; topic_group: 'SEMANTIC'|'PART_OF_SPEECH'; }

export interface TopicRepository {
  findAll(): Promise<TopicRow[]>;
  findCatalog(): Promise<TopicCatalogRow[]>;
  findByCode(code: string): Promise<TopicRow | undefined>;
}

export function createTopicRepository(pool: Pool): TopicRepository {
  return {
    async findAll() {
      const result = await pool.query<TopicRow>(
        'SELECT id, code, name, description, image_url FROM topics ORDER BY name, id',
      );
      return result.rows;
    },
    async findCatalog() {
      const result = await pool.query<TopicCatalogRow>(
        `SELECT t.id,t.code,t.name,t.description,t.image_url,
           CASE WHEN t.code LIKE 'POS\\_%' ESCAPE '\\' THEN 'PART_OF_SPEECH' ELSE 'SEMANTIC' END topic_group,
           COUNT(DISTINCT f.id) FILTER (WHERE f.is_active=TRUE)::text AS flashcard_count
         FROM topics t LEFT JOIN flashcard_topics ft ON ft.topic_id=t.id
         LEFT JOIN flashcards f ON f.id=ft.flashcard_id
         WHERE t.code<>'GENERAL_VOCABULARY'
         GROUP BY t.id,t.code,t.name,t.description,t.image_url
         HAVING COUNT(DISTINCT f.id) FILTER (WHERE f.is_active=TRUE)>0
         ORDER BY CASE WHEN t.code LIKE 'POS\\_%' ESCAPE '\\' THEN 1 ELSE 0 END,t.name,t.id`,
      );
      return result.rows;
    },
    async findByCode(code) {
      const result = await pool.query<TopicRow>(
        'SELECT id, code, name, description, image_url FROM topics WHERE code = $1',
        [code],
      );
      return result.rows[0];
    },
  };
}
