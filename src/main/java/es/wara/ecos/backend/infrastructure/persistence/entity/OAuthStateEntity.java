package es.wara.ecos.backend.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA que mapea la tabla oauth_states.
 * Almacena tokens de estado para el flujo OAuth de Spotify, protegiendo el callback contra CSRF.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Entity
@Table(name = "oauth_states")
@Getter
@Setter
@NoArgsConstructor
public class OAuthStateEntity {

    @Id
    private String state;

    @Column(name = "user_id")
    private UUID userId;

    /** LINK = vincular Spotify a cuenta existente · LOGIN = autenticarse con Spotify */
    @Column(name = "purpose", nullable = false, length = 10)
    private String purpose = "LINK";

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

}