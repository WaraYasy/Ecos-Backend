-- V4: Tags culturales (Last.fm)
-- Etiquetas comunitarias por canción: géneros, moods, estéticas.
-- weight indica relevancia del tag (mayor = más representativo).

CREATE TABLE song_tags (
    song_id BIGINT      REFERENCES songs(id) ON DELETE CASCADE,
    tag     VARCHAR(100) NOT NULL,
    weight  DECIMAL(5,3) DEFAULT 1.0,
    PRIMARY KEY (song_id, tag)
);