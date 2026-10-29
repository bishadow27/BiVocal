-- Up Migration
-- topics and flashcards are the existing baseline. Never recreate or reseed them.
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password_hash TEXT NOT NULL,
    full_name VARCHAR(100),
    avatar_url TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP,
    CONSTRAINT users_username_format CHECK (username ~ '^[A-Za-z0-9_]{3,30}$'),
    CONSTRAINT users_email_trimmed CHECK (email = BTRIM(email) AND email <> '')
);
CREATE UNIQUE INDEX idx_users_email_unique ON users (LOWER(email));
CREATE UNIQUE INDEX idx_users_username_unique ON users (LOWER(username));

-- Down Migration
-- Drops only the table, indexes and SERIAL sequence owned by this migration.
-- Run explicitly in development only; user records will be lost.
DROP TABLE users;
