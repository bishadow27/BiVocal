-- Up Migration
-- Historical naive timestamps were written by CURRENT_TIMESTAMP in the database session timezone.
-- Preserve their instants when moving review dates to timezone-aware storage.
ALTER TABLE user_progress
  ALTER COLUMN last_review TYPE TIMESTAMPTZ USING last_review AT TIME ZONE current_setting('TimeZone'),
  ALTER COLUMN next_review TYPE TIMESTAMPTZ USING next_review AT TIME ZONE current_setting('TimeZone');

CREATE INDEX idx_user_progress_due
  ON user_progress (user_id, next_review, flashcard_id)
  WHERE review_count > 0;

-- Down Migration
DROP INDEX idx_user_progress_due;
ALTER TABLE user_progress
  ALTER COLUMN last_review TYPE TIMESTAMP USING last_review AT TIME ZONE current_setting('TimeZone'),
  ALTER COLUMN next_review TYPE TIMESTAMP USING next_review AT TIME ZONE current_setting('TimeZone');
