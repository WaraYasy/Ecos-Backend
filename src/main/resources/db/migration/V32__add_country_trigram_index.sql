-- V32: Índice trigrama en artists.country para acelerar el autocompletado de países.
-- Sin este índice, la query ILIKE '%q%' fuerza un seq scan sobre todos los artistas.
-- Con gin_trgm_ops el coste es O(log n) independientemente del wildcard inicial.

CREATE INDEX IF NOT EXISTS idx_artists_country_trgm
    ON artists USING GIN (country gin_trgm_ops);