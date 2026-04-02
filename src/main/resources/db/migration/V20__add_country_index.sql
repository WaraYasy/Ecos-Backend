-- Índice para búsquedas geográficas por país de origen del artista (case-insensitive)
CREATE INDEX IF NOT EXISTS idx_artists_country_lower ON artists (lower(country)) WHERE country IS NOT NULL;
