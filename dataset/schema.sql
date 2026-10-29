-- PostgreSQL 14+; execute in pgAdmin Query Tool before seed scripts.
BEGIN;
-- ICU is included with the standard PostgreSQL Windows distribution.
-- Unicode casing must work even when the database was created with locale C.
CREATE COLLATION IF NOT EXISTS dataset_unicode
    (provider = icu, locale = 'und', deterministic = true);
CREATE TABLE IF NOT EXISTS topics (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    image_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CHECK (code = BTRIM(code) AND code ~ '^[A-Z][A-Z0-9_]*$')
);
CREATE TABLE IF NOT EXISTS flashcards (
    id SERIAL PRIMARY KEY,
    topic_id INTEGER NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    english VARCHAR(150) COLLATE dataset_unicode NOT NULL,
    vietnamese VARCHAR(255) COLLATE dataset_unicode NOT NULL,
    pronunciation VARCHAR(150),
    part_of_speech VARCHAR(50),
    example TEXT,
    example_vi TEXT,
    level VARCHAR(10),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CHECK (english = BTRIM(english) AND english <> ''),
    CHECK (vietnamese = BTRIM(vietnamese) AND vietnamese <> ''),
    CHECK (level IS NULL OR level IN ('A1', 'A2', 'B1', 'B2')),
    CHECK (part_of_speech IS NULL OR part_of_speech IN
        ('noun', 'verb', 'adjective', 'adverb', 'preposition', 'conjunction', 'pronoun', 'phrase'))
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_flashcards_unique_word_meaning
ON flashcards (LOWER(english), LOWER(vietnamese));
CREATE INDEX IF NOT EXISTS idx_flashcards_topic_id ON flashcards(topic_id);
-- These checks prevent space variants from bypassing the requested unique index.
-- Existing tables are not migrated by CREATE TABLE IF NOT EXISTS.
COMMIT;
