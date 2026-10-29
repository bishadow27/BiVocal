-- Up Migration
-- users/topics/flashcards already exist. Do not recreate or reseed the baseline.
CREATE TABLE user_progress (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    flashcard_id INTEGER NOT NULL REFERENCES flashcards(id) ON DELETE CASCADE,
    memory_level SMALLINT NOT NULL DEFAULT 0,
    review_count INTEGER NOT NULL DEFAULT 0,
    correct_count INTEGER NOT NULL DEFAULT 0,
    wrong_count INTEGER NOT NULL DEFAULT 0,
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    last_review TIMESTAMP,
    next_review TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_progress_memory_level_range CHECK (memory_level BETWEEN 0 AND 5),
    CONSTRAINT user_progress_review_count_nonnegative CHECK (review_count >= 0),
    CONSTRAINT user_progress_correct_count_nonnegative CHECK (correct_count >= 0),
    CONSTRAINT user_progress_wrong_count_nonnegative CHECK (wrong_count >= 0)
);
CREATE UNIQUE INDEX idx_user_progress_user_flashcard_unique
    ON user_progress (user_id, flashcard_id);
CREATE INDEX idx_user_progress_user_id ON user_progress (user_id);
CREATE INDEX idx_user_progress_flashcard_id ON user_progress (flashcard_id);

-- Down Migration
-- Only the objects owned by this migration. No CASCADE to baseline tables.
DROP TABLE user_progress;
