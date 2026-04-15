-- V20: Índices en columnas FK sin indexar
-- playlists.user_id, sessions.user_id y session_songs.session_id son consultadas
-- frecuentemente con findByUser_Id() / findBySession_Id() pero carecían de índice,
-- provocando sequential scans en cada petición autenticada.

CREATE INDEX idx_playlists_user_id        ON playlists(user_id);
CREATE INDEX idx_sessions_user_id         ON sessions(user_id);
CREATE INDEX idx_session_songs_session_id ON session_songs(session_id);