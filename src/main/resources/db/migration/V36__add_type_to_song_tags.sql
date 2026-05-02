
-- V35: Añadir columna type a song_tags
-- Clasifica cada tag como 'genre', 'aesthetic' o 'other'.
-- Los tags existentes quedan como 'other'; los nuevos se clasifican en código.

ALTER TABLE song_tags ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'other';