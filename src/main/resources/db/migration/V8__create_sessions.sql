-- V8: Sesiones de exploración (rutas del grafo)
-- Una sesión guarda la ruta que el usuario navegó en el grafo neuronal.
-- session_nodes: cada canción visitada con su posición visual (x, y).
-- session_edges: conexiones entre nodos con el valor de similitud calculado.

CREATE TABLE sessions (
    id            UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID      REFERENCES users(id) ON DELETE CASCADE,
    start_song_id UUID      REFERENCES songs(id),
    created_at    TIMESTAMP DEFAULT NOW()
);

CREATE TABLE session_nodes (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id     UUID         REFERENCES sessions(id) ON DELETE CASCADE,
    song_id        UUID         REFERENCES songs(id),
    x_position     DECIMAL(8,4),
    y_position     DECIMAL(8,4),
    visited_at     TIMESTAMP    DEFAULT NOW(),
    sequence_order SMALLINT     NOT NULL
);

CREATE TABLE session_edges (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id   UUID         REFERENCES sessions(id) ON DELETE CASCADE,
    from_song_id UUID         REFERENCES songs(id),
    to_song_id   UUID         REFERENCES songs(id),
    similarity   DECIMAL(5,4)  -- valor 0.0000 a 1.0000
);