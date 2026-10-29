-- Up Migration
CREATE TABLE user_scores (
    user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    total_xp INTEGER NOT NULL DEFAULT 0 CHECK (total_xp >= 0),
    current_streak INTEGER NOT NULL DEFAULT 0 CHECK (current_streak >= 0),
    longest_streak INTEGER NOT NULL DEFAULT 0 CHECK (longest_streak >= 0),
    last_activity_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_scores_longest_at_least_current CHECK (longest_streak >= current_streak)
);
CREATE TABLE xp_history (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    flashcard_id INTEGER REFERENCES flashcards(id) ON DELETE SET NULL,
    quiz_result_id INTEGER REFERENCES quiz_results(id) ON DELETE SET NULL,
    source_type VARCHAR(20) NOT NULL CHECK (source_type IN ('QUIZ', 'REVIEW')),
    xp_amount INTEGER NOT NULL CHECK (xp_amount > 0),
    activity_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- QUIZ requires an ID at creation in recordLearningActivity. It may become
    -- NULL after FK SET NULL; a permanent NOT NULL check would block deletion.
    CONSTRAINT xp_history_review_has_no_quiz CHECK (source_type <> 'REVIEW' OR quiz_result_id IS NULL)
);
CREATE INDEX idx_user_scores_total_xp ON user_scores (total_xp DESC);
CREATE INDEX idx_xp_history_user_id ON xp_history (user_id);
CREATE INDEX idx_xp_history_user_created_at ON xp_history (user_id, created_at DESC);
CREATE INDEX idx_xp_history_flashcard_id ON xp_history (flashcard_id);
CREATE UNIQUE INDEX idx_xp_history_daily_award_unique
    ON xp_history (user_id, source_type, flashcard_id, activity_date)
    WHERE flashcard_id IS NOT NULL;
CREATE UNIQUE INDEX idx_xp_history_quiz_result_unique
    ON xp_history (quiz_result_id) WHERE quiz_result_id IS NOT NULL;

-- Down Migration
DROP TABLE xp_history;
DROP TABLE user_scores;
