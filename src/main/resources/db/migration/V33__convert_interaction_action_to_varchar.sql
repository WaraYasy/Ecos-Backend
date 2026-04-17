-- V33: Convertir action de interaction_action (enum PostgreSQL) a VARCHAR.
-- Hibernate 6 envía el valor como character varying; PostgreSQL no hace cast implícito
-- a tipos enum personalizados, lo que causaba ERROR 42804 en cada inserción.
-- La validación de valores válidos se mantiene en el dominio Java (InteractionAction enum).

ALTER TABLE user_interactions
    ALTER COLUMN action TYPE VARCHAR(50) USING action::VARCHAR;

DROP TYPE IF EXISTS interaction_action;