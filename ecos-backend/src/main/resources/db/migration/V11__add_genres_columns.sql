-- V11: Añadir columna genres a artists y movies
-- genres almacena una lista de strings como JSONB (ej. ["rock", "indie", "alternative"])
-- artists.genres: géneros musicales del artista (fuente: Spotify)
-- movies.genres:  géneros cinematográficos de la película (fuente: TMDb)

ALTER TABLE artists ADD COLUMN genres JSONB;
ALTER TABLE movies  ADD COLUMN genres JSONB;