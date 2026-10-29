-- Up Migration
CREATE TABLE vocabulary_sources (
  id SERIAL PRIMARY KEY,
  code VARCHAR(80) NOT NULL UNIQUE,
  name TEXT NOT NULL,
  version TEXT NOT NULL,
  source_url TEXT NOT NULL,
  license TEXT NOT NULL,
  checksum_sha256 CHAR(64) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE flashcard_provenance (
  flashcard_id INTEGER NOT NULL REFERENCES flashcards(id) ON DELETE CASCADE,
  source_id INTEGER NOT NULL REFERENCES vocabulary_sources(id) ON DELETE RESTRICT,
  source_record_id TEXT NOT NULL,
  source_word_id BIGINT,
  source_definition_id BIGINT,
  cefr_scope TEXT NOT NULL,
  validation_status TEXT NOT NULL CHECK (validation_status IN ('APPROVED_AUTO','NEEDS_REVIEW','REJECTED')),
  validation_reasons JSONB NOT NULL DEFAULT '[]'::jsonb,
  quiz_eligible BOOLEAN NOT NULL DEFAULT FALSE,
  imported_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (flashcard_id, source_id),
  UNIQUE (source_id, source_record_id)
);

CREATE TABLE flashcard_topics (
  flashcard_id INTEGER NOT NULL REFERENCES flashcards(id) ON DELETE CASCADE,
  topic_id INTEGER NOT NULL REFERENCES topics(id) ON DELETE RESTRICT,
  is_primary BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (flashcard_id, topic_id)
);

INSERT INTO flashcard_topics (flashcard_id, topic_id, is_primary)
SELECT id, topic_id, TRUE FROM flashcards
ON CONFLICT (flashcard_id, topic_id) DO NOTHING;

CREATE UNIQUE INDEX idx_flashcard_topics_one_primary
  ON flashcard_topics(flashcard_id) WHERE is_primary;
CREATE INDEX idx_flashcard_topics_topic_card ON flashcard_topics(topic_id, flashcard_id);
CREATE INDEX idx_flashcards_level_active ON flashcards(level, id) WHERE is_active = TRUE;
CREATE INDEX idx_flashcards_english_lower ON flashcards(LOWER(english));
CREATE INDEX idx_flashcards_vietnamese_lower ON flashcards(LOWER(vietnamese));

-- Down Migration
DROP INDEX idx_flashcards_vietnamese_lower;
DROP INDEX idx_flashcards_english_lower;
DROP INDEX idx_flashcards_level_active;
DROP INDEX idx_flashcard_topics_topic_card;
DROP INDEX idx_flashcard_topics_one_primary;
DROP TABLE flashcard_topics;
DROP TABLE flashcard_provenance;
DROP TABLE vocabulary_sources;
