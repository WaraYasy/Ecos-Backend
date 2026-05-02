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
 * Especificaciones JPA para la búsqueda multidimensional de canciones en los cuadrantes de ECOS.
 *
 * <p>Función principal {@link #from(SearchParams)} combina con {@code AND} solo los
 * predicados cuyo parámetro no es nulo. Los cuadrantes soportados son:</p>
 * <ul>
 *   <li><b>Artista</b> ({@code artist}): EXISTS subquery sobre {@code artists.name} — evita
 *       producto cartesiano con el cuadrante de texto libre cuando ambos están activos</li>
 *   <li><b>Texto libre</b> ({@code q}): busca en título de la canción y nombre del artista</li>
 *   <li><b>Estética</b> ({@code aesthetic}): filtra por tag exacto en {@code song_tags}</li>
 *   <li><b>Género musical</b> ({@code genre}): filtra por tag en {@code song_tags} (coincidencia parcial)</li>
 *   <li><b>Temporal</b> ({@code decade}): filtra por año del álbum; soporta "1980s", "1985" y "1980-1990"</li>
 *   <li><b>Cinematográfico</b> ({@code movie}): busca canciones asociadas a una película</li>
 *   <li><b>Geográfico</b> ({@code country}): EXISTS subquery sobre {@code artists.country}</li>
 *   <li><b>Textura</b> ({@code texture}): filtra por tags de textura sonora en {@code song_tags}</li>
 *   <li><b>Técnico</b> ({@code energyMin/Max}, {@code valenceMin/Max}, {@code bpmMin/Max},
 *       {@code instrumental}, {@code mode}): INNER JOIN con {@code audio_features} —
 *       excluye automáticamente canciones sin datos de audio</li>
 * </ul>
 *
 * <p><b>EXISTS vs JOIN para artistas/país:</b> los filtros de {@code artist} y {@code country}
 * usan subconsultas correlacionadas {@code EXISTS} en lugar de {@code JOIN} directo sobre
 * {@code root} para evitar producto cartesiano cuando el cuadrante de texto libre ya ha
 * añadido un {@code JOIN} sobre la tabla {@code artists}.</p>
 *
 * @author Wara Pacheco
 * @version 1.1
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

            // ── Artista (filtro directo por nombre de artista) ────────────────
            if (params.artist() != null && !params.artist().isBlank()) {
                query.distinct(true);
                String pattern = "%" + params.artist().toLowerCase() + "%";
                Subquery<UUID> sub = query.subquery(UUID.class);
                Root<SongEntity> subRoot = sub.from(SongEntity.class);
                Join<SongEntity, ArtistEntity> subArtistJoin = subRoot.join("artists");
                sub.select(subRoot.get("id"));
                sub.where(cb.and(
                        cb.equal(subRoot.get("id"), root.get("id")),
                        cb.like(cb.lower(subArtistJoin.get("name")), pattern)
                ));
                predicates.add(cb.exists(sub));
            }

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
            // LIKE en vez de equals: los tags de DB son géneros de Spotify, no etiquetas
            // estéticas exactas — la coincidencia parcial amplía el resultado sin perder relevancia
            if (params.aesthetic() != null && !params.aesthetic().isBlank()) {
                Subquery<Integer> sub = query.subquery(Integer.class);
                Root<SongTagEntity> tagRoot = sub.from(SongTagEntity.class);
                sub.select(cb.literal(1));
                String pattern = "%" + params.aesthetic().toLowerCase() + "%";
                sub.where(cb.and(
                        cb.equal(tagRoot.get("id").get("songId"), root.get("id")),
                        cb.like(tagRoot.get("id").get("tag"), pattern)
                ));
                predicates.add(cb.exists(sub));
            }

            // ── Cuadrante 2b: Género musical ─────────────────────────────────
            // EXISTS + LIKE: con idx_song_tags_tag_trgm (GIN trigrama) evita seq-scan
            if (params.genre() != null && !params.genre().isBlank()) {
                Subquery<Integer> sub = query.subquery(Integer.class);
                Root<SongTagEntity> tagRoot = sub.from(SongTagEntity.class);
                sub.select(cb.literal(1));
                String pattern = "%" + params.genre().toLowerCase() + "%";
                sub.where(cb.and(
                        cb.equal(tagRoot.get("id").get("songId"), root.get("id")),
                        cb.like(tagRoot.get("id").get("tag"), pattern)
                ));
                predicates.add(cb.exists(sub));
            }

            // ── Cuadrante 3: Temporal ─────────────────────────────────────────
            // decade: solo "1980s" → [1980, 1989] (cuadrante seed, validado en controller)
            // year:   "1985" → [1985, 1985]  |  "1980-1990" → [1980, 1990]
            String temporalParam = (params.decade() != null && !params.decade().isBlank())
                    ? params.decade().trim()
                    : (params.year() != null && !params.year().isBlank()) ? params.year().trim() : null;
            if (temporalParam != null) {
                int[] range = parseTemporalRange(temporalParam);
                if (range != null) {
                    Join<SongEntity, AlbumEntity> albumJoin = root.join("album", JoinType.LEFT);
                    predicates.add(cb.between(
                            albumJoin.<Short>get("year"),
                            (short) range[0],
                            (short) range[1]
                    ));
                }
            }

            // ── Cuadrante 4: Cinematográfico ──────────────────────────────────
            if (params.movie() != null && !params.movie().isBlank()) {
                query.distinct(true);
                Join<SongEntity, MovieEntity> movieJoin = root.join("movies", JoinType.LEFT);
                String pattern = "%" + params.movie().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(movieJoin.get("title")), pattern));
            }

            // ── Cuadrante 6: Geográfico (artistas de ese país de origen) ─────
            if (params.country() != null && !params.country().isBlank()) {
                query.distinct(true);
                String code = params.country().toUpperCase();
                Subquery<UUID> sub = query.subquery(UUID.class);
                Root<SongEntity> subRoot = sub.from(SongEntity.class);
                Join<SongEntity, ArtistEntity> subArtistJoin = subRoot.join("artists");
                sub.select(subRoot.get("id"));
                sub.where(cb.and(
                        cb.equal(subRoot.get("id"), root.get("id")),
                        cb.equal(cb.upper(subArtistJoin.get("country")), code)
                ));
                predicates.add(cb.exists(sub));
            }

            // ── Cuadrante 5a: Textura (filtro por tags — independiente de audio features) ──
            // EXISTS correlacionado con IN(lista fija): la lista es pequeña (≤5 tags),
            // así que IN sobre valores literales es eficiente y PostgreSQL puede usar
            // idx_song_tags_song_id_tag para la búsqueda por song_id.
            if (params.texture() != null && !params.texture().isBlank()) {
                List<String> textureTags = resolveTextureTags(params.texture());
                if (!textureTags.isEmpty()) {
                    Subquery<Integer> sub = query.subquery(Integer.class);
                    Root<SongTagEntity> tagRoot = sub.from(SongTagEntity.class);
                    sub.select(cb.literal(1));
                    sub.where(cb.and(
                            cb.equal(tagRoot.get("id").get("songId"), root.get("id")),
                            tagRoot.get("id").get("tag").in(textureTags)
                    ));
                    predicates.add(cb.exists(sub));
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
     * Parsea el parámetro temporal y devuelve un array [startYear, endYear].
     * Devuelve {@code null} si el formato no es reconocido.
     *
     * <ul>
     *   <li>{@code "1980s"}     → [1980, 1989] (decade)</li>
     *   <li>{@code "1985"}      → [1985, 1985] (year exacto)</li>
     *   <li>{@code "1980-1990"} → [1980, 1990] (year rango)</li>
     * </ul>
     */
    private static int[] parseTemporalRange(String value) {
        try {
            if (value.endsWith("s")) {
                int start = Integer.parseInt(value.substring(0, value.length() - 1));
                return new int[]{start, start + 9};
            } else if (value.contains("-")) {
                String[] parts = value.split("-", 2);
                int start = Integer.parseInt(parts[0].trim());
                int end   = Integer.parseInt(parts[1].trim());
                return new int[]{Math.min(start, end), Math.max(start, end)};
            } else {
                int year = Integer.parseInt(value);
                return new int[]{year, year};
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
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
