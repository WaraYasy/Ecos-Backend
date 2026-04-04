-- V25: Habilitar extensión pgvector y crear tabla de embeddings semánticos
-- CREATE EXTENSION es idempotente (IF NOT EXISTS) — seguro en dev, test y prod.

CREATE EXTENSION IF NOT EXISTS vector;

-- Embeddings semánticos por canción
-- Vectores de 384 dimensiones generados por el microservicio IA.
-- Usados para calcular similitud semántica (componente 0.25 de la fórmula del grafo).
-- El índice ivfflat permite búsqueda de vecinos más cercanos por coseno eficientemente.

CREATE TABLE song_embeddings (
    song_id       UUID         PRIMARY KEY REFERENCES songs(id) ON DELETE CASCADE,
    embedding     vector(384)  NOT NULL,
    model_version VARCHAR(50),
    updated_at    TIMESTAMP    DEFAULT now()
);

CREATE INDEX ON song_embeddings USING ivfflat (embedding vector_cosine_ops);