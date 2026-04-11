-- V29: Simplificar modelo de sesiones de exploración
--
-- La lógica del grafo vive en el frontend — no tiene sentido persistir
-- coordenadas visuales (x, y) ni aristas (session_edges) en el backend.
-- Una sesión es simplemente una lista ordenada de canciones exploradas.
--
-- Cambios:
--   · sessions: añadir columna name (nombre opcional dado por el usuario)
--   · session_nodes: eliminar x_position, y_position, visited_at
--   · session_nodes: renombrar a session_songs
--   · session_edges: eliminar tabla completa (no se usa)

ALTER TABLE sessions ADD COLUMN name VARCHAR(100);

DROP TABLE IF EXISTS session_edges;

ALTER TABLE session_nodes DROP COLUMN IF EXISTS x_position;
ALTER TABLE session_nodes DROP COLUMN IF EXISTS y_position;
ALTER TABLE session_nodes DROP COLUMN IF EXISTS visited_at;

ALTER TABLE session_nodes RENAME TO session_songs;