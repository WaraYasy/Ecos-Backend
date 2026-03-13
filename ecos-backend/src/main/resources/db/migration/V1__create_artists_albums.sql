-- V1: Artistas y álbumes
-- Base del catálogo musical. Las canciones dependen de estas tablas.

CREATE TABLE artists (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    spotify_id VARCHAR(50)  UNIQUE NOT NULL,
    name       VARCHAR(255) NOT NULL,
    country    VARCHAR(100)
);

CREATE TABLE albums (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    spotify_id VARCHAR(50)  UNIQUE NOT NULL,
    title      VARCHAR(255) NOT NULL,
    year       SMALLINT,
    cover_url  VARCHAR(500),
    artist_id  UUID REFERENCES artists(id)
);