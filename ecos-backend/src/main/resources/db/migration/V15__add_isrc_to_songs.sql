-- V15: Añadir columna isrc a songs para deduplicación cross-grabación.
-- El ISRC (International Standard Recording Code) identifica de forma única
-- una grabación específica, independientemente del servicio de origen.
-- Permite detectar y evitar duplicados entre versiones remasterizadas, regionales, etc.
ALTER TABLE songs ADD COLUMN isrc VARCHAR(12);
CREATE UNIQUE INDEX idx_songs_isrc ON songs (isrc) WHERE isrc IS NOT NULL;
