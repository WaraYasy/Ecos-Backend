-- Añade columna para almacenar el hash bcrypt de la contraseña del usuario.
-- El DEFAULT '' permite migrar usuarios existentes sin romper NOT NULL.
ALTER TABLE users ADD COLUMN password_hash VARCHAR(255) NOT NULL DEFAULT '';
