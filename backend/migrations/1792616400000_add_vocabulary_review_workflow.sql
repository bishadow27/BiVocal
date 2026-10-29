-- Up Migration
ALTER TABLE flashcard_provenance DROP CONSTRAINT flashcard_provenance_validation_status_check;
ALTER TABLE flashcard_provenance ADD CONSTRAINT flashcard_provenance_validation_status_check
  CHECK (validation_status IN ('APPROVED_AUTO','APPROVED_MANUAL','NEEDS_REVIEW','REJECTED'));

CREATE TABLE vocabulary_review_topics (
  code VARCHAR(50) PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  description TEXT,
  source_payload JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE vocabulary_review_candidates (
  candidate_id VARCHAR(80) PRIMARY KEY,
  english TEXT NOT NULL,
  vietnamese TEXT NOT NULL,
  pronunciation TEXT,
  part_of_speech VARCHAR(30),
  cefr VARCHAR(2) CHECK (cefr IN ('A1','A2','B1','B2')),
  primary_topic_code VARCHAR(50),
  topic_codes TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
  example TEXT,
  example_vi TEXT,
  source_code VARCHAR(80) NOT NULL,
  source_name TEXT NOT NULL,
  source_url TEXT NOT NULL,
  source_version TEXT NOT NULL,
  source_license TEXT NOT NULL,
  source_record_id TEXT NOT NULL,
  source_word_id BIGINT,
  source_definition_id BIGINT,
  cefr_scope TEXT NOT NULL,
  validation_reasons TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
  quality_score INTEGER NOT NULL DEFAULT 0,
  quality_verified BOOLEAN NOT NULL DEFAULT FALSE,
  license_status TEXT NOT NULL CHECK (license_status IN ('ATTRIBUTION_REQUIRED','VERIFIED','BLOCKED')),
  status TEXT NOT NULL CHECK (status IN ('PENDING','APPROVED','REJECTED','PUBLISHED')),
  reviewed_by INTEGER REFERENCES users(id) ON DELETE RESTRICT,
  reviewed_at TIMESTAMPTZ,
  published_flashcard_id INTEGER REFERENCES flashcards(id) ON DELETE RESTRICT,
  row_version INTEGER NOT NULL DEFAULT 1,
  original_payload JSONB NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(source_code, source_record_id)
);

CREATE TABLE vocabulary_publish_batches (
  batch_id VARCHAR(36) PRIMARY KEY,
  fingerprint CHAR(64) NOT NULL UNIQUE,
  status TEXT NOT NULL CHECK (status IN ('PREVIEWED','PUBLISHING','COMPLETED','FAILED')),
  created_by INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  confirmed_by INTEGER REFERENCES users(id) ON DELETE RESTRICT,
  preview JSONB NOT NULL,
  inserted_count INTEGER NOT NULL DEFAULT 0,
  skipped_count INTEGER NOT NULL DEFAULT 0,
  blocked_count INTEGER NOT NULL DEFAULT 0,
  error_code TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  confirmed_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ
);

CREATE TABLE vocabulary_publish_batch_items (
  batch_id VARCHAR(36) NOT NULL REFERENCES vocabulary_publish_batches(batch_id) ON DELETE RESTRICT,
  candidate_id VARCHAR(80) NOT NULL REFERENCES vocabulary_review_candidates(candidate_id) ON DELETE RESTRICT,
  candidate_version INTEGER NOT NULL,
  disposition TEXT NOT NULL CHECK (disposition IN ('INSERT','SKIP_DUPLICATE','BLOCKED')),
  duplicate_flashcard_id INTEGER REFERENCES flashcards(id) ON DELETE RESTRICT,
  reason TEXT,
  PRIMARY KEY(batch_id,candidate_id)
);

CREATE TABLE vocabulary_review_audit (
  audit_id BIGSERIAL PRIMARY KEY,
  candidate_id VARCHAR(80) REFERENCES vocabulary_review_candidates(candidate_id) ON DELETE RESTRICT,
  actor_user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  action TEXT NOT NULL CHECK (action IN ('EDIT','APPROVE','REJECT','RESET_PENDING','BULK_APPROVE','BULK_REJECT','BULK_RESET_PENDING','PREVIEW','PUBLISH','PUBLISH_SKIPPED')),
  before_value JSONB,
  after_value JSONB,
  batch_id VARCHAR(36) REFERENCES vocabulary_publish_batches(batch_id) ON DELETE RESTRICT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vocabulary_review_status_id ON vocabulary_review_candidates(status,candidate_id);
CREATE INDEX idx_vocabulary_review_filters ON vocabulary_review_candidates(cefr,primary_topic_code,part_of_speech);
CREATE INDEX idx_vocabulary_review_english ON vocabulary_review_candidates(LOWER(english));
CREATE INDEX idx_vocabulary_review_vietnamese ON vocabulary_review_candidates(LOWER(vietnamese));
CREATE INDEX idx_vocabulary_review_reasons ON vocabulary_review_candidates USING GIN(validation_reasons);
CREATE INDEX idx_vocabulary_review_audit_candidate ON vocabulary_review_audit(candidate_id,created_at DESC);

-- Down Migration
DROP INDEX idx_vocabulary_review_audit_candidate;
DROP INDEX idx_vocabulary_review_reasons;
DROP INDEX idx_vocabulary_review_vietnamese;
DROP INDEX idx_vocabulary_review_english;
DROP INDEX idx_vocabulary_review_filters;
DROP INDEX idx_vocabulary_review_status_id;
DROP TABLE vocabulary_review_audit;
DROP TABLE vocabulary_publish_batch_items;
DROP TABLE vocabulary_publish_batches;
DROP TABLE vocabulary_review_candidates;
DROP TABLE vocabulary_review_topics;
ALTER TABLE flashcard_provenance DROP CONSTRAINT flashcard_provenance_validation_status_check;
ALTER TABLE flashcard_provenance ADD CONSTRAINT flashcard_provenance_validation_status_check
  CHECK (validation_status IN ('APPROVED_AUTO','NEEDS_REVIEW','REJECTED'));
