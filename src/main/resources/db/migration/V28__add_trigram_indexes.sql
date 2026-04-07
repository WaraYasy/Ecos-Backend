-- V28: Índices trigrama (pg_trgm) para acelerar búsquedas LIKE '%term%'
--
-- El planner de PostgreSQL no puede usar un índice btree con wildcards iniciales
-- (LIKE '%rock%'). Con GIN + gin_trgm_ops el coste pasa de seq-scan
-- a búsqueda por índice, crítico con 100k+ canciones.
--
-- También añade un índice compuesto (song_id, tag) que acelera las subconsultas
-- EXISTS correlacionadas que reemplazan el IN(subquery) en SongSpecification.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Búsqueda parcial en títulos de canciones (cuadrante texto libre)
CREATE INDEX IF NOT EXISTS idx_songs_title_trgm
    ON songs USING GIN(title gin_trgm_ops);

-- Búsqueda parcial en nombre de artista (cuadrante texto libre y artista)
CREATE INDEX IF NOT EXISTS idx_artists_name_trgm
    ON artists USING GIN(name gin_trgm_ops);

-- Búsqueda parcial en tags (cuadrante género y estética)
CREATE INDEX IF NOT EXISTS idx_song_tags_tag_trgm
    ON song_tags USING GIN(tag gin_trgm_ops);

-- Índice compuesto para EXISTS(SELECT 1 FROM song_tags WHERE song_id = ? AND tag LIKE ?)
-- Permite lookup por song_id + filtro de tag en una sola pasada de índice
CREATE INDEX IF NOT EXISTS idx_song_tags_song_id_tag
    ON song_tags(song_id, tag);