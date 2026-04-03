-- V23: Limpiar URLs de preview de Deezer en canciones existentes.
-- El CDN de Deezer (dzcdn.net) bloquea peticiones de AVFoundation (iOS, error -12860).
-- Las nuevas canciones obtendrán preview de iTunes Search API.
-- Las existentes con URL de Deezer se limpian para que MusicAggregationService
-- las rellene desde iTunes en el próximo enriquecimiento.
UPDATE songs
SET preview_url = NULL
WHERE preview_url LIKE '%dzcdn.net%';