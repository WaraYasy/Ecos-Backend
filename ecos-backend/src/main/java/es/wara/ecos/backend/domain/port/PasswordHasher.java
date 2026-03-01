package es.wara.ecos.backend.domain.port;

/**
 * Puerto de salida para el hashing y verificación de contraseñas.
 * Abstrae la implementación concreta (BCrypt u otra) del dominio de aplicación.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public interface PasswordHasher {

    /**
     * Genera el hash de la contraseña en texto plano.
     *
     * @param raw contraseña en texto plano
     * @return hash de la contraseña
     */
    String hash(String raw);

    /**
     * Comprueba si una contraseña en texto plano coincide con su hash.
     *
     * @param raw    contraseña en texto plano
     * @param hashed hash almacenado
     * @return {@code true} si coinciden
     */
    boolean matches(String raw, String hashed);

}
