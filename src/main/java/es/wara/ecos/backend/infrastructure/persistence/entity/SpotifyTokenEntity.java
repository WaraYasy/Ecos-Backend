package es.wara.ecos.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA que mapea la tabla {@code spotify_tokens}.
 * <p>
 * Almacena los tokens OAuth de Spotify vinculados a un usuario de ECOS.
 * El access_token y el refresh_token se guardan cifrados con AES-256.
 * La clave primaria es el mismo UUID del usuario (relación 1:1 vía {@code @MapsId}).
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Entity
@Table(name = "spotify_tokens")
@Getter
@Setter
@NoArgsConstructor
public class SpotifyTokenEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    /** Relación 1:1 con el usuario propietario del token. */
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserEntity user;

    /** Token de acceso cifrado con AES-256 (válido ~1h, renovable con refreshToken). */
    @Column(name = "access_token", columnDefinition = "TEXT", nullable = false)
    private String accessToken;

    /** Token de refresco cifrado con AES-256 (larga duración; usado para renovar el access_token). */
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    /** Instante de expiración del access_token en UTC. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Ámbitos autorizados por el usuario (ej. {@code "playlist-modify-public"}). */
    @Column(length = 500)
    private String scope;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}