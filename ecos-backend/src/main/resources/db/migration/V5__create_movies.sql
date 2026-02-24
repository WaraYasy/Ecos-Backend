-- V5: Películas y vinculación con canciones (TMDb)
-- Permite filtros cinematográficos y el modo "Cinematográfico" del grafo.

CREATE TABLE movies (
    id       BIGSERIAL    PRIMARY KEY,
    tmdb_id  INTEGER      UNIQUE NOT NULL,
    title    VARCHAR(255) NOT NULL,
    year     SMALLINT,
    director VARCHAR(255)
);

-- Relación N:M: una canción puede aparecer en varias películas y viceversa
CREATE TABLE song_movies (
    song_id  BIGINT REFERENCES songs(id)   ON DELETE CASCADE,
    movie_id BIGINT REFERENCES movies(id)  ON DELETE CASCADE,
    PRIMARY KEY (song_id, movie_id)
);