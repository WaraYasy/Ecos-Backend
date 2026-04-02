-- V19: Índices únicos necesarios para upserts con ON CONFLICT DO NOTHING
--
-- Problema: múltiples hilos concurrentes intentaban insertar el mismo álbum/artista
-- simultáneamente (race condition en findOrCreate). Con ON CONFLICT DO NOTHING el INSERT
-- es atómico: solo gana un hilo y los demás no fallan, simplemente no insertan.
--
-- Requiere índices únicos en las columnas usadas como conflict target.

-- Álbumes: dedup por cover_url (fuente Deezer — la URL contiene el ID del álbum en Deezer)
CREATE UNIQUE INDEX IF NOT EXISTS idx_albums_cover_url
    ON albums (cover_url) WHERE cover_url IS NOT NULL;

-- Artistas sin spotify_id (fuente Deezer): dedup por nombre en minúsculas
-- El índice es parcial (WHERE spotify_id IS NULL) para no afectar artistas de Spotify,
-- que se deduplan por su propio idx_artists_spotify_id.
CREATE UNIQUE INDEX IF NOT EXISTS idx_artists_name_lower
    ON artists (lower(name)) WHERE spotify_id IS NULL;
