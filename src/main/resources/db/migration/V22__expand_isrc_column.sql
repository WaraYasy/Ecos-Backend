-- V22: Expandir isrc de VARCHAR(12) a VARCHAR(15) para admitir ISRCs con guiones.
-- El formato ISRC estándar sin guiones es 12 chars (ej: USRC17607839).
-- Con guiones el formato tiene 15 chars (ej: US-RC1-76-07839).
-- Spotify devuelve ambos formatos; VARCHAR(12) causaba "value too long" para los segundos.
ALTER TABLE songs ALTER COLUMN isrc TYPE VARCHAR(15);