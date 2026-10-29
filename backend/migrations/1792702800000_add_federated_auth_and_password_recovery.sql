-- Up Migration
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE users ADD COLUMN auth_version INTEGER NOT NULL DEFAULT 1
  CHECK (auth_version >= 1);

CREATE TABLE user_auth_identities (
  id BIGSERIAL PRIMARY KEY,
  user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  provider VARCHAR(32) NOT NULL,
  provider_subject VARCHAR(255) NOT NULL,
  email_at_link VARCHAR(150),
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_login_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT user_auth_identities_provider_check CHECK (provider = 'GOOGLE'),
  CONSTRAINT user_auth_identities_provider_subject_unique UNIQUE (provider, provider_subject),
  CONSTRAINT user_auth_identities_user_provider_unique UNIQUE (user_id, provider)
);
CREATE INDEX idx_user_auth_identities_user_id ON user_auth_identities(user_id);

CREATE TABLE google_auth_challenges (
  id UUID PRIMARY KEY,
  nonce_hash CHAR(64) NOT NULL UNIQUE,
  expires_at TIMESTAMPTZ NOT NULL,
  used_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_google_auth_challenges_expires_at ON google_auth_challenges(expires_at);

CREATE TABLE password_reset_tokens (
  id BIGSERIAL PRIMARY KEY,
  user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash CHAR(64) NOT NULL UNIQUE,
  expires_at TIMESTAMPTZ NOT NULL,
  used_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_password_reset_tokens_user_active
  ON password_reset_tokens(user_id, expires_at) WHERE used_at IS NULL;

-- Down Migration
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM users WHERE password_hash IS NULL) THEN
    RAISE EXCEPTION 'Cannot restore password_hash NOT NULL while Google-only users exist';
  END IF;
END $$;
DROP TABLE password_reset_tokens;
DROP TABLE google_auth_challenges;
DROP TABLE user_auth_identities;
ALTER TABLE users DROP COLUMN auth_version;
ALTER TABLE users ALTER COLUMN password_hash SET NOT NULL;
