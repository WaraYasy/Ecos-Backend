package es.wara.ecos.backend.infrastructure.adapter.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO que representa los créditos de una película en TMDb.
 * <p>
 * Se deserializa desde {@code GET /movie/{id}/credits}.
 * Contiene el reparto (cast) y el equipo técnico (crew).
 * El director se extrae del crew filtrando por {@code job == "Director"}.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TMDBCreditDTO {

    /** ID de la película a la que pertenecen estos créditos. */
    private Long id;

    /** Reparto de la película. */
    private List<CastMember> cast;

    /** Equipo técnico de la película (directores, productores, guionistas, etc.). */
    private List<CrewMember> crew;

    /**
     * Miembro del reparto (actor/actriz).
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CastMember {
        /** ID del miembro del reparto en TMDb. */
        private Long id;

        /** Nombre real del actor/actriz. */
        private String name;

        /** Nombre del personaje interpretado. */
        private String character;

        /** Orden de aparición en los créditos (0 = protagonista). */
        private Integer order;
    }

    /**
     * Miembro del equipo técnico.
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CrewMember {
        /** ID del miembro del equipo en TMDb. */
        private Long id;

        /** Nombre del miembro del equipo. */
        private String name;

        /** Rol desempeñado (ej. "Director", "Producer", "Screenplay"). */
        private String job;

        /** Departamento al que pertenece (ej. "Directing", "Production"). */
        private String department;
    }
}