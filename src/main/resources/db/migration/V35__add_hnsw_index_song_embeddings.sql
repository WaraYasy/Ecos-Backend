-- V35: Índice HNSW sobre song_embeddings para búsqueda de vecinos más próximos
--
-- Sin índice, cada búsqueda de similitud coseno hace un full scan de todos los
-- embeddings. HNSW no requiere parámetro de entrenamiento (a diferencia de IVFFlat),
-- lo que lo hace adecuado desde el primer uso sin conocer el volumen final del catálogo.
--
-- m=16, ef_construction=64 son los valores por defecto de pgvector y un buen punto
-- de partida: buena precisión con coste de construcción bajo.

CREATE INDEX IF NOT EXISTS idx_song_embeddings_hnsw
    ON song_embeddings
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);