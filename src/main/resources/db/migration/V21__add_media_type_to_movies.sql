-- V20: Añadir media_type a movies para soportar series (TV) además de películas
-- TMDb usa espacios de IDs independientes para movie y tv, por lo que el UNIQUE
-- debe ser sobre (tmdb_id, media_type) para evitar colisiones entre ambos tipos.

ALTER TABLE movies ADD COLUMN media_type VARCHAR(10) NOT NULL DEFAULT 'movie';

-- Reemplazar unique constraint solo sobre tmdb_id por una compuesta
ALTER TABLE movies DROP CONSTRAINT movies_tmdb_id_key;
ALTER TABLE movies ADD CONSTRAINT movies_tmdb_id_media_type_key UNIQUE (tmdb_id, media_type);