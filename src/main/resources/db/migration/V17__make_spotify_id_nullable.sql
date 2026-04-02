-- V17: Hacer spotify_id nullable para permitir persistir canciones de fuentes
-- que no tienen cruce con Spotify (ej. Last.fm cuando Spotify devuelve 429).
-- La deduplicación se mantiene por ISRC (V15) como primera clave, y por
-- spotify_id cuando está disponible, mediante índice único parcial.

-- 1. Eliminar el índice único existente (no parcial) para spotify_id
DROP INDEX IF EXISTS idx_songs_spotify_id;

-- 2. Quitar el NOT NULL constraint de spotify_id
ALTER TABLE songs ALTER COLUMN spotify_id DROP NOT NULL;

-- 3. Recrear el índice único como parcial (solo filas con spotify_id no nulo)
CREATE UNIQUE INDEX idx_songs_spotify_id ON songs (spotify_id) WHERE spotify_id IS NOT NULL;
