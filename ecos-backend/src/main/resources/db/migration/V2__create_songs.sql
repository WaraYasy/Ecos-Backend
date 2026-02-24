-- V2: Canciones y relación canción-artista
-- Una canción pertenece a un álbum y puede tener varios artistas (colaboraciones).

CREATE TABLE songs (
    id          BIGSERIAL    PRIMARY KEY,
    spotify_id  VARCHAR(50)  UNIQUE NOT NULL,
    title       VARCHAR(255) NOT NULL,
    duration_ms INTEGER,
    preview_url VARCHAR(500),
    album_id    BIGINT REFERENCES albums(id),
    created_at  TIMESTAMP    DEFAULT NOW()
);

-- Relación N:M entre canciones y artistas (feat., colaboraciones)
CREATE TABLE song_artists (
    song_id   BIGINT REFERENCES songs(id)   ON DELETE CASCADE,
    artist_id BIGINT REFERENCES artists(id) ON DELETE CASCADE,
    PRIMARY KEY (song_id, artist_id)
);