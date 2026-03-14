-- V6: Usuarios y perfiles musicales
-- user_profiles calcula la "huella musical" del usuario a partir de su historial.
-- favorite_eras y top_genres se almacenan como JSONB para flexibilidad.

CREATE TABLE users (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username   VARCHAR(100) UNIQUE NOT NULL,
    email      VARCHAR(255) UNIQUE NOT NULL,
    language   VARCHAR(10)  DEFAULT 'es',
    created_at TIMESTAMP    DEFAULT NOW()
);

CREATE TABLE user_profiles (
    user_id           UUID         PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    avg_energy        DECIMAL(4,3),
    harmonic_richness DECIMAL(4,3),
    eclecticity       DECIMAL(4,3),
    favorite_eras     JSONB,         -- ej. {"1980s": 0.4, "1990s": 0.3}
    top_genres        JSONB          -- ej. ["electronic", "pop", "indie"]
);