package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.SpotifyTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositorio Spring Data JPA para {@link SpotifyTokenEntity}.
 * <p>
 * Extiende {@code JpaRepository} con la PK {@code UUID} (que coincide con el
 * {@code user_id} del usuario propietario). Las operaciones de búsqueda y guardado
 * del token Spotify se delegan a este repositorio desde {@code SpotifyAuthService}.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public interface JpaSpotifyTokenRepository extends JpaRepository<SpotifyTokenEntity, UUID> {
}
