-- V9: Interacciones de usuario con canciones
-- Alimenta el microservicio IA para calcular perfiles y recomendaciones.
-- action registra qué hizo el usuario: reproducir, saltar, añadir a playlist, expandir nodo.

CREATE TYPE interaction_action AS ENUM ('play', 'skip', 'add_playlist', 'expand_node');

CREATE TABLE user_interactions (
    id          UUID               PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID               REFERENCES users(id) ON DELETE CASCADE,
    song_id     UUID               REFERENCES songs(id) ON DELETE CASCADE,
    action      interaction_action NOT NULL,
    duration_ms INTEGER,            -- milisegundos escuchados (relevante en 'play')
    created_at  TIMESTAMP          DEFAULT NOW()
);