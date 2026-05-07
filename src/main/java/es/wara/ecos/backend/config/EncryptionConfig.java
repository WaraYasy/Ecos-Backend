package es.wara.ecos.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

/**
 * Configuración del cifrado simétrico (AES-256-GCM) para datos sensibles en BD.
 * <p>
 * Utilizado para cifrar/descifrar tokens de OAuth (Spotify) antes de persistirlos.
 * Las claves se inyectan desde variables de entorno — nunca en código ni en Git.
 * </p>
 *
 * @author Wara Pacheco
 */
@Configuration
public class EncryptionConfig {

    /**
     * Bean de cifrado AES-256-GCM. La clave se lee de {@code ENCRYPTION_KEY}
     * y el salt (hex de 16 chars) de {@code ENCRYPTION_SALT}.
     */
    @Bean
    public TextEncryptor textEncryptor(
            @Value("${encryption.key}") String key,
            @Value("${encryption.salt}") String salt) {
        return Encryptors.text(key, salt);
    }
}
