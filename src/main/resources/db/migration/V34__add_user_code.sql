-- Añade código de usuario corto y legible para crossover colaborativo (ECO-XXXX-XXXX)
ALTER TABLE users ADD COLUMN user_code VARCHAR(13);

-- Genera códigos únicos para usuarios existentes usando row_number para garantizar unicidad
WITH numbered AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY created_at) AS rn FROM users
)
UPDATE users
SET user_code = 'ECO-'
    || UPPER(LPAD(TO_HEX(n.rn::int), 4, '0'))
    || '-'
    || UPPER(LPAD(TO_HEX((n.rn * 37 + 999)::int), 4, '0'))
FROM numbered n
WHERE users.id = n.id;

ALTER TABLE users ALTER COLUMN user_code SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT users_user_code_key UNIQUE (user_code);
CREATE INDEX idx_users_user_code ON users (user_code);