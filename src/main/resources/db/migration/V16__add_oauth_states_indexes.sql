-- V16: Índices en oauth_states para acelerar limpieza de estados expirados
-- y consultas por usuario.
CREATE INDEX idx_oauth_states_user_id    ON oauth_states(user_id);
CREATE INDEX idx_oauth_states_expires_at ON oauth_states(expires_at);