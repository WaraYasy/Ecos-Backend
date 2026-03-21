package es.wara.ecos.backend.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * Servicio de generación y validación de tokens JWT para autenticación stateless en ECOS.
 *
 * <p>Usa la librería <b>jjwt 0.12.6</b> con algoritmo HMAC-SHA256. El claim {@code sub}
 * almacena el {@code userId} como String UUID; {@code iat} y {@code exp} siguen el estándar JWT.</p>
 *
 * <p>Configuración (variables de entorno / properties):
 * <ul>
 *   <li>{@code JWT_SECRET} — secreto HMAC en texto plano (mínimo 32 caracteres recomendado)</li>
 *   <li>{@code JWT_EXPIRATION_MS} — tiempo de vida en ms; por defecto 86 400 000 (24 h)</li>
 * </ul>
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Component
public class JwtService {

    /** Clave secreta en texto plano inyectada desde la variable de entorno {@code JWT_SECRET}. */
    @Value("${jwt.secret}")
    private String secret;

    /** Tiempo de vida del token en milisegundos. Por defecto 24 h (86 400 000 ms). */
    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    /**
     * Construye la clave HMAC-SHA a partir del secreto configurado.
     * <p>
     * Se recalcula en cada llamada para reflejar posibles rotaciones de secreto
     * sin necesidad de reiniciar el contexto de Spring.
     * </p>
     *
     * @return clave criptográfica lista para firmar o verificar tokens
     */
    private SecretKey getKey() {
        // Convierte el secreto UTF-8 en una SecretKey apta para HMAC-SHA256/384/512
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Genera un token JWT con el userId como sujeto.
     *
     * @param userId identificador del usuario
     * @return token JWT firmado con HMAC-SHA y listo para enviar al cliente
     */
    public String generateToken(UUID userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId.toString())   // sujeto = userId en formato String
                .issuedAt(now)                // momento de emisión (claim "iat")
                .expiration(expiry)           // momento de expiración (claim "exp")
                .signWith(getKey())           // firma HMAC-SHA256
                .compact();                   // serializa a string compacto Base64URL
    }

    /**
     * Extrae el userId del token JWT.
     * <p>
     * Lanza una excepción de jjwt (p.ej. {@code ExpiredJwtException}) si el token
     * ha expirado o la firma no es válida.
     * </p>
     *
     * @param token token JWT válido y firmado
     * @return identificador del usuario contenido en el claim {@code sub}
     */
    public UUID extractUserId(String token) {
        // Parsea y verifica el token; lanza excepción si es inválido o expirado
        Claims claims = Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // El sujeto se almacenó como String al generar el token
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Valida la firma y la expiración del token JWT.
     * <p>
     * Devuelve {@code false} para cualquier error de jjwt: firma incorrecta,
     * token expirado, formato malformado, etc.
     * </p>
     *
     * @param token token a validar
     * @return {@code true} si el token es válido y no ha expirado
     */
    public boolean isValid(String token) {
        try {
            // Intentar parsear el token: si no lanza excepción, es válido
            Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            // Cualquier excepción indica token inválido (expirado, malformado, firma incorrecta…)
            return false;
        }
    }

}
