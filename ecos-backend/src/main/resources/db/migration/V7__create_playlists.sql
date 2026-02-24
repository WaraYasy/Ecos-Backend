-- V7: Playlists y canciones de playlist
-- position permite ordenar las canciones (drag & drop en el frontend).
-- spotify_export_id se rellena cuando se exporta a Spotify.

CREATE TABLE playlists (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       REFERENCES users(id) ON DELETE CASCADE,
    name             VARCHAR(255) NOT NULL,
    description      TEXT,
    spotify_export_id VARCHAR(50),
    created_at       TIMESTAMP    DEFAULT NOW()
);

CREATE TABLE playlist_songs (
    playlist_id BIGINT   REFERENCES playlists(id) ON DELETE CASCADE,
    song_id     BIGINT   REFERENCES songs(id)     ON DELETE CASCADE,
    position    SMALLINT NOT NULL,
    PRIMARY KEY (playlist_id, song_id)
);