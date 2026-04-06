-- V27: Soporte para login con Spotify (OAuth como método de autenticación)
--
-- Añade auth_provider y provider_id a users para identificar el origen de la cuenta.
-- password_hash pasa a ser nullable porque los usuarios de Spotify no tienen contraseña local.
-- oauth_states recibe un campo purpose (LINK | LOGIN) y user_id pasa a ser nullable
-- porque en el flujo LOGIN aún no conocemos el userId cuando generamos el state.

ALTER TABLE users
    ADD COLUMN auth_provider VARCHAR(20) NOT NULL DEFAULT 'local',
    ADD COLUMN provider_id   VARCHAR(100),
    ALTER COLUMN password_hash DROP NOT NULL;

CREATE UNIQUE INDEX idx_users_provider_id
    ON users(provider_id) WHERE provider_id IS NOT NULL;

ALTER TABLE oauth_states
    ADD COLUMN purpose VARCHAR(10) NOT NULL DEFAULT 'LINK',
    ALTER COLUMN user_id DROP NOT NULL;