-- Tokens de usuario de Spotify (Authorization Code Flow)
-- Sustituye el almacenamiento en memoria (ConcurrentHashMap) por persistencia real.
-- access_token y refresh_token se almacenan cifrados (AES-256 vía Spring Security Crypto).
CREATE TABLE spotify_tokens (
    user_id       BIGINT      PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    access_token  TEXT        NOT NULL,
    refresh_token TEXT,
    expires_at    TIMESTAMPTZ NOT NULL,
    scope         VARCHAR(500),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
