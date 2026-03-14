-- V5: Películas y vinculación con canciones (TMDb)
-- Permite filtros cinematográficos y el modo "Cinematográfico" del grafo.

CREATE TABLE movies (
    id       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tmdb_id  INTEGER      UNIQUE NOT NULL,
    title    VARCHAR(255) NOT NULL,
    year     SMALLINT,
    director VARCHAR(255)
);

-- Relación N:M: una canción puede aparecer en varias películas y viceversa
CREATE TABLE song_movies (
    song_id  UUID REFERENCES songs(id)   ON DELETE CASCADE,
    movie_id UUID REFERENCES movies(id)  ON DELETE CASCADE,
    PRIMARY KEY (song_id, movie_id)
);