-- V30: Ampliar columna tag en song_tags de VARCHAR(100) a VARCHAR(255)
-- Motivo: algunos genre_hint del CSV superan los 100 caracteres.
ALTER TABLE song_tags ALTER COLUMN tag TYPE VARCHAR(255);