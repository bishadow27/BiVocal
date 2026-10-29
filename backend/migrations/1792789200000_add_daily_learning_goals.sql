-- Up Migration
-- M15 stores account-scoped goals and exact post-M15 learning events.
CREATE TABLE user_learning_settings (
  user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  daily_goal SMALLINT NOT NULL DEFAULT 10 CHECK (daily_goal IN (5, 10, 15, 20)),
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_learned_flashcards (
  user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  flashcard_id INTEGER NOT NULL REFERENCES flashcards(id) ON DELETE CASCADE,
  first_learned_at TIMESTAMPTZ,
  first_source VARCHAR(16) NOT NULL CHECK (first_source IN ('REVIEW', 'QUIZ', 'LEGACY')),
  PRIMARY KEY (user_id, flashcard_id),
  CHECK ((first_source = 'LEGACY' AND first_learned_at IS NULL)
      OR (first_source IN ('REVIEW', 'QUIZ') AND first_learned_at IS NOT NULL))
);

CREATE TABLE user_learning_daily (
  user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  learning_date DATE NOT NULL,
  goal_snapshot SMALLINT NOT NULL CHECK (goal_snapshot IN (5, 10, 15, 20)),
  new_words_count INTEGER NOT NULL DEFAULT 0 CHECK (new_words_count >= 0),
  review_count INTEGER NOT NULL DEFAULT 0 CHECK (review_count >= 0),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, learning_date)
);

CREATE TABLE learning_system_config (
  singleton BOOLEAN PRIMARY KEY DEFAULT TRUE CHECK (singleton),
  activated_date DATE NOT NULL
);
INSERT INTO learning_system_config (singleton, activated_date)
VALUES (TRUE, (CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Ho_Chi_Minh')::date);

-- Existing review/quiz evidence prevents a historical card from being counted as new later.
-- No historical date is invented: legacy rows deliberately keep first_learned_at NULL.
INSERT INTO user_learned_flashcards (user_id, flashcard_id, first_learned_at, first_source)
SELECT DISTINCT evidence.user_id, evidence.flashcard_id, NULL::TIMESTAMPTZ, 'LEGACY'
FROM (
  SELECT user_id, flashcard_id FROM user_progress WHERE review_count > 0
  UNION
  SELECT user_id, flashcard_id FROM quiz_results
) evidence;

CREATE INDEX idx_user_learned_flashcards_flashcard
  ON user_learned_flashcards (flashcard_id, user_id);
CREATE INDEX idx_user_learning_daily_user_date
  ON user_learning_daily (user_id, learning_date DESC);

-- Down Migration
DROP TABLE learning_system_config;
DROP TABLE user_learning_daily;
DROP TABLE user_learned_flashcards;
DROP TABLE user_learning_settings;
