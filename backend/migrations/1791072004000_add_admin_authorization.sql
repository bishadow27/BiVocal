-- Up Migration
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER'
    CONSTRAINT users_role_check CHECK (role IN ('USER', 'ADMIN'));

-- Down Migration
ALTER TABLE users DROP COLUMN role;
