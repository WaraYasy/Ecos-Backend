-- V10: Índices para búsqueda y rendimiento
-- GIN para búsqueda full-text en títulos.
-- Índices en columnas usadas frecuentemente en WHERE y JOIN.

-- Búsqueda full-text en título de canción
CREATE INDEX idx_songs_title_fts ON songs USING GIN(to_tsvector('english', title));

-- FKs de songs y albums (acelera JOINs)
CREATE INDEX idx_songs_album_id      ON songs(album_id);
CREATE INDEX idx_albums_artist_id    ON albums(artist_id);
CREATE INDEX idx_song_artists_artist ON song_artists(artist_id);

-- Filtros por características de audio (búsqueda multidimensional)
CREATE INDEX idx_audio_features_energy  ON audio_features(energy);
CREATE INDEX idx_audio_features_valence ON audio_features(valence);
CREATE INDEX idx_audio_features_tempo   ON audio_features(tempo);

-- Búsqueda de canciones por tag (filtro estético/Last.fm)
CREATE INDEX idx_song_tags_tag ON song_tags(tag);

-- Historial de interacciones por usuario (para IA y perfil)
CREATE INDEX idx_interactions_user_id ON user_interactions(user_id);
CREATE INDEX idx_interactions_song_id ON user_interactions(song_id);
CREATE INDEX idx_interactions_action  ON user_interactions(action);