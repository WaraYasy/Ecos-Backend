-- V24: Índice en spotify_tokens.expires_at
-- Evita full table scan al limpiar tokens expirados o al verificar expiración.

CREATE INDEX idx_spotify_tokens_expires_at ON spotify_tokens(expires_at);