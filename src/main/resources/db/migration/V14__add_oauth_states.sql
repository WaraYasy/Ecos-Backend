-- V14: Tabla para almacenar state tokens de OAuth de Spotify.
-- Protege el callback contra ataques CSRF al vincular el state a un userId
-- con un TTL de 10 minutos.
CREATE TABLE oauth_states (
    state      VARCHAR(64)  PRIMARY KEY,
    user_id    UUID         NOT NULL,
    expires_at TIMESTAMP    NOT NULL
);
