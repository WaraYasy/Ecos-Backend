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

public interface JpaArtistRepository extends JpaRepository<ArtistEntity, UUID> {

    Optional<ArtistEntity> findBySpotifyId(String spotifyId);

    Optional<ArtistEntity> findByNameIgnoreCase(String name);

    List<ArtistEntity> findTop6ByNameContainingIgnoreCase(String name);

    // ILIKE usa el índice trigrama idx_artists_country_trgm (V32); limitado a 10 para UI
    @Query(value = "SELECT DISTINCT country FROM artists " +
                   "WHERE country IS NOT NULL AND country ILIKE CONCAT('%', :q, '%') " +
                   "ORDER BY country LIMIT 10",
           nativeQuery = true)
    List<String> findDistinctCountriesContaining(@Param("q") String q);

    @Query("SELECT a FROM ArtistEntity a WHERE a.country IS NULL AND a.name IS NOT NULL")
    Page<ArtistEntity> findWithoutCountry(Pageable pageable);

}
