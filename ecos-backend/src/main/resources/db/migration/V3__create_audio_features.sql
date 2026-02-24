-- V3: Características de audio (Spotify)
-- Relación 1:1 con songs. Datos técnicos que usa el motor de similitud.
-- Valores normalizados 0.0-1.0 salvo tempo (BPM) y loudness (dB).

CREATE TABLE audio_features (
    song_id          BIGINT       PRIMARY KEY REFERENCES songs(id) ON DELETE CASCADE,
    energy           DECIMAL(4,3),  -- 0.0 a 1.0
    tempo            DECIMAL(6,3),  -- BPM (ej. 120.5)
    valence          DECIMAL(4,3),  -- 0.0 (triste) a 1.0 (eufórico)
    danceability     DECIMAL(4,3),  -- 0.0 a 1.0
    acousticness     DECIMAL(4,3),  -- 0.0 a 1.0
    instrumentalness DECIMAL(4,3),  -- 0.0 a 1.0
    speechiness      DECIMAL(4,3),  -- 0.0 a 1.0
    loudness         DECIMAL(6,3),  -- dB (típicamente -60 a 0)
    musical_key      SMALLINT,      -- 0=C, 1=C#, 2=D... 11=B
    mode             SMALLINT,      -- 0=menor, 1=mayor
    time_signature   SMALLINT       -- compás (3, 4, 5...)
);