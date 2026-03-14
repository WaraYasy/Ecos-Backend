package es.wara.ecos.backend.infrastructure.security;

import es.wara.ecos.backend.domain.port.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Implementación del puerto {@link PasswordHasher} usando BCrypt a través de Spring Security.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Component
public class BcryptPasswordHasher implements PasswordHasher {

    /** Implementación concreta de BCrypt proporcionada por el bean de Spring Security. */
    private final PasswordEncoder encoder;

    /**
     * Inyecta el {@link PasswordEncoder} definido en {@code SecurityConfig}.
     *
     * @param encoder encoder BCrypt configurado en el contexto de Spring
     */
    public BcryptPasswordHasher(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    /**
     * Genera el hash BCrypt de la contraseña en texto plano.
     * <p>
     * BCrypt incorpora un salt aleatorio en cada llamada, por lo que el resultado
     * varía entre invocaciones para la misma entrada.
     * </p>
     *
     * @param raw contraseña en texto plano
     * @return hash BCrypt listo para persistir (incluye el salt embebido)
     */
    @Override
    public String hash(String raw) {
        return encoder.encode(raw);
    }

    /**
     * Comprueba si una contraseña en texto plano corresponde a un hash BCrypt almacenado.
     * <p>
     * Extrae el salt del {@code hashed} para rehacer el hash de {@code raw} y compararlos
     * de forma segura (comparación en tiempo constante).
     * </p>
     *
     * @param raw    contraseña en texto plano introducida por el usuario
     * @param hashed hash BCrypt almacenado en base de datos
     * @return {@code true} si la contraseña coincide con el hash
     */
    @Override
    public boolean matches(String raw, String hashed) {
        return encoder.matches(raw, hashed);
    }

}
