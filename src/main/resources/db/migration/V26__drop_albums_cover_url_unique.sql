-- V26: Eliminar índice único en albums.cover_url
--
-- El índice se creó en V19 asumiendo que las URLs de portada de Deezer
-- contenían el ID del álbum y eran únicas. Con iTunes la misma imagen
-- puede aparecer en álbumes distintos (reediciones, portadas genéricas),
-- lo que provoca "duplicate key" al importar CSVs enriquecidos con iTunes.
--
-- La deduplicación de álbumes se gestiona por album_spotify_id (idx_albums_spotify_id),
-- que sí es un identificador real y único.

DROP INDEX IF EXISTS idx_albums_cover_url;