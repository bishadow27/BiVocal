-- Up Migration
-- Existing users/user_progress/topics/flashcards are an applied baseline.
CREATE TABLE quiz_results (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    flashcard_id INTEGER NOT NULL REFERENCES flashcards(id) ON DELETE CASCADE,
    selected_flashcard_id INTEGER REFERENCES flashcards(id) ON DELETE SET NULL,
    is_correct BOOLEAN NOT NULL,
    question_type VARCHAR(30) NOT NULL DEFAULT 'EN_TO_VI',
    answered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT quiz_results_question_type CHECK (question_type = 'EN_TO_VI')
);
CREATE INDEX idx_quiz_results_user_id ON quiz_results (user_id);
CREATE INDEX idx_quiz_results_user_answered_at ON quiz_results (user_id, answered_at DESC);
CREATE INDEX idx_quiz_results_flashcard_id ON quiz_results (flashcard_id);

-- Down Migration
-- Only this migration's table/indexes/owned SERIAL sequence. No CASCADE.
DROP TABLE quiz_results;
