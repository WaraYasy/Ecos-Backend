package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.ArtistEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para {@link ArtistEntity}.
 * <p>
 * Además de las operaciones CRUD heredadas de {@code JpaRepository}, expone
 * consultas específicas de dominio para búsqueda por Spotify ID, por nombre
 * (ignorando mayúsculas) y para sugerencias de autocompletado de artistas y países.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.1
 * @since 1.0.0
 */
public interface JpaArtistRepository extends JpaRepository<ArtistEntity, UUID> {

    Optional<ArtistEntity> findBySpotifyId(String spotifyId);

    Optional<ArtistEntity> findByNameIgnoreCase(String name);

    /** Devuelve hasta 6 artistas cuyo nombre contiene {@code name} (case-insensitive). */
    List<ArtistEntity> findTop6ByNameContainingIgnoreCase(String name);

    /** Devuelve todos los códigos ISO de país distintos de artistas presentes en la BD. */
    @Query(value = "SELECT DISTINCT country FROM artists WHERE country IS NOT NULL ORDER BY country",
           nativeQuery = true)
    List<String> findAllDistinctCountries();

    /** Devuelve artistas sin país asignado para el trabajo de enriquecimiento. */
    @Query("SELECT a FROM ArtistEntity a WHERE a.country IS NULL AND a.name IS NOT NULL")
    Page<ArtistEntity> findWithoutCountry(Pageable pageable);

}
