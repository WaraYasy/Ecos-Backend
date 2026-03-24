package es.wara.ecos.backend.infrastructure.persistence.specification;

import es.wara.ecos.backend.application.usecase.search.SearchParams;
import es.wara.ecos.backend.infrastructure.persistence.entity.AlbumEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.ArtistEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.AudioFeaturesEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.MovieEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.SongEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.SongTagEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Especificaciones JPA para la búsqueda multidimensional de canciones en los 5 cuadrantes de ECOS.
 *
 * <p>Función principal {@link #from(SearchParams)} combina con {@code AND} solo los
 * predicados cuyo parámetro no es nulo. Los cuadrantes soportados son:</p>
 * <ul>
 *   <li><b>Texto libre</b> ({@code q}): busca en título de la canción y nombre del artista</li>
 *   <li><b>Estética</b> ({@code aesthetic}): filtra por tag en {@code song_tags}</li>
 *   <li><b>Género musical</b> ({@code genre}): filtra por tag en {@code song_tags} (coincidencia parcial)</li>
 *   <li><b>Temporal</b> ({@code decade}): filtra por año del álbum (ej. "1980s" → 1980-1989)</li>
 *   <li><b>Cinematográfico</b> ({@code movie}): busca canciones asociadas a una película</li>
 *   <li><b>Técnico</b>: filtra por audio features (INNER JOIN, excluye canciones sin datos)</li>
 * </ul>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public class SongSpecification {

    private SongSpecification() {}

    /**
     * Construye una {@link Specification} combinando todos los cuadrantes activos con AND.
     *
     * @param params parámetros de búsqueda; los campos nulos se ignoran
     * @return especificación lista para usar con {@code JpaSpecificationExecutor.findAll}
     */
    public static Specification<SongEntity> from(SearchParams params) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // ── Cuadrante 1: Texto libre ──────────────────────────────────────
            if (params.q() != null && !params.q().isBlank()) {
                query.distinct(true);
                Join<SongEntity, ArtistEntity> artistJoin = root.join("artists", JoinType.LEFT);
                String pattern = "%" + params.q().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(artistJoin.get("name")), pattern)
                ));
            }

            // ── Cuadrante 2: Estética ─────────────────────────────────────────
            if (params.aesthetic() != null && !params.aesthetic().isBlank()) {
                Subquery<UUID> sub = query.subquery(UUID.class);
                Root<SongTagEntity> tagRoot = sub.from(SongTagEntity.class);
                sub.select(tagRoot.get("id").get("songId"));
                sub.where(cb.equal(tagRoot.get("id").get("tag"), params.aesthetic().toLowerCase()));
                predicates.add(root.get("id").in(sub));
            }

            // ── Cuadrante 2b: Género musical ─────────────────────────────────
            if (params.genre() != null && !params.genre().isBlank()) {
                Subquery<UUID> sub = query.subquery(UUID.class);
                Root<SongTagEntity> tagRoot = sub.from(SongTagEntity.class);
                sub.select(tagRoot.get("id").get("songId"));
                String pattern = "%" + params.genre().toLowerCase() + "%";
                sub.where(cb.like(tagRoot.get("id").get("tag"), pattern));
                predicates.add(root.get("id").in(sub));
            }

            // ── Cuadrante 3: Temporal ─────────────────────────────────────────
            if (params.decade() != null && !params.decade().isBlank()) {
                try {
                    int startYear = Integer.parseInt(params.decade().replace("s", "").trim());
                    Join<SongEntity, AlbumEntity> albumJoin = root.join("album", JoinType.LEFT);
                    predicates.add(cb.between(
                            albumJoin.<Short>get("year"),
                            (short) startYear,
                            (short) (startYear + 9)
                    ));
                } catch (NumberFormatException ignored) {
                    // Formato de decade inválido — ignorar este predicado
                }
            }

            // ── Cuadrante 4: Cinematográfico ──────────────────────────────────
            if (params.movie() != null && !params.movie().isBlank()) {
                query.distinct(true);
                Join<SongEntity, MovieEntity> movieJoin = root.join("movies", JoinType.LEFT);
                String pattern = "%" + params.movie().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(movieJoin.get("title")), pattern));
            }

            // ── Cuadrante 5a: Textura (filtro por tags — independiente de audio features) ──
            if (params.texture() != null && !params.texture().isBlank()) {
                List<String> textureTags = resolveTextureTags(params.texture());
                if (!textureTags.isEmpty()) {
                    Subquery<UUID> sub = query.subquery(UUID.class);
                    Root<SongTagEntity> tagRoot = sub.from(SongTagEntity.class);
                    sub.select(tagRoot.get("id").get("songId"));
                    sub.where(tagRoot.get("id").get("tag").in(textureTags));
                    predicates.add(root.get("id").in(sub));
                }
            }

            // ── Cuadrante 5b: Técnico numérico (INNER JOIN — excluye canciones sin audio features) ──
            boolean hasAudioFeatureFilters = params.instrumental() != null
                    || params.mode() != null
                    || params.energyMin() != null || params.energyMax() != null
                    || params.valenceMin() != null || params.valenceMax() != null
                    || params.bpmMin() != null || params.bpmMax() != null;

            if (hasAudioFeatureFilters) {
                Join<SongEntity, AudioFeaturesEntity> afJoin = root.join("audioFeatures", JoinType.INNER);

                if (params.instrumental() != null) {
                    if (params.instrumental()) {
                        predicates.add(cb.ge(afJoin.<BigDecimal>get("instrumentalness"), new BigDecimal("0.8")));
                    } else {
                        predicates.add(cb.lt(afJoin.<BigDecimal>get("instrumentalness"), new BigDecimal("0.5")));
                    }
                }

                if (params.mode() != null) {
                    if ("major".equalsIgnoreCase(params.mode())) {
                        predicates.add(cb.equal(afJoin.<Short>get("mode"), (short) 1));
                    } else if ("minor".equalsIgnoreCase(params.mode())) {
                        predicates.add(cb.equal(afJoin.<Short>get("mode"), (short) 0));
                    }
                }

                if (params.energyMin() != null) {
                    predicates.add(cb.ge(afJoin.<BigDecimal>get("energy"), params.energyMin()));
                }
                if (params.energyMax() != null) {
                    predicates.add(cb.le(afJoin.<BigDecimal>get("energy"), params.energyMax()));
                }
                if (params.valenceMin() != null) {
                    predicates.add(cb.ge(afJoin.<BigDecimal>get("valence"), params.valenceMin()));
                }
                if (params.valenceMax() != null) {
                    predicates.add(cb.le(afJoin.<BigDecimal>get("valence"), params.valenceMax()));
                }
                if (params.bpmMin() != null) {
                    predicates.add(cb.ge(afJoin.<BigDecimal>get("tempo"), params.bpmMin()));
                }
                if (params.bpmMax() != null) {
                    predicates.add(cb.le(afJoin.<BigDecimal>get("tempo"), params.bpmMax()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Resuelve los tags de BD asociados a una textura musical.
     *
     * @param texture nombre de textura: orchestral, electronic, acoustic o ambient
     * @return lista de tags a buscar en {@code song_tags}; vacía si la textura es desconocida
     */
    private static List<String> resolveTextureTags(String texture) {
        return switch (texture.toLowerCase()) {
            case "orchestral" -> List.of("orchestral", "classical", "strings", "cinematic", "symphonic");
            case "electronic" -> List.of("electronic", "synth", "synthesizer", "edm", "techno");
            case "acoustic"   -> List.of("acoustic", "folk", "unplugged");
            case "ambient"    -> List.of("ambient", "atmospheric", "drone", "soundscape");
            default           -> List.of();
        };
    }
}
