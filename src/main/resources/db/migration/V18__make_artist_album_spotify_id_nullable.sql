-- V18: Hacer spotify_id nullable en artists y albums para soportar fuentes
-- alternativas a Spotify (ej. Deezer). La unicidad se mantiene como índice
-- parcial WHERE spotify_id IS NOT NULL, igual que V17 hizo con songs.

-- ── Artists ──────────────────────────────────────────────────────────────────
ALTER TABLE artists DROP CONSTRAINT IF EXISTS artists_spotify_id_key;
ALTER TABLE artists ALTER COLUMN spotify_id DROP NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_artists_spotify_id
    ON artists (spotify_id) WHERE spotify_id IS NOT NULL;

-- ── Albums ───────────────────────────────────────────────────────────────────
ALTER TABLE albums DROP CONSTRAINT IF EXISTS albums_spotify_id_key;
ALTER TABLE albums ALTER COLUMN spotify_id DROP NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_albums_spotify_id
    ON albums (spotify_id) WHERE spotify_id IS NOT NULL;
