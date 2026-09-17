-- Application users, provisioned on first Google OAuth2 login.
CREATE TABLE users (
    id          UUID         PRIMARY KEY,
    google_sub  VARCHAR(255) NOT NULL UNIQUE,
    email       VARCHAR(320) NOT NULL UNIQUE,
    name        VARCHAR(255),
    avatar_url  VARCHAR(1024),
    role        VARCHAR(32)  NOT NULL DEFAULT 'USER',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email ON users (email);
