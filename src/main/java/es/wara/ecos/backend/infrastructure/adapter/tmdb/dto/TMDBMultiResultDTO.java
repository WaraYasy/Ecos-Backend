package es.wara.ecos.backend.infrastructure.adapter.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para un resultado del endpoint {@code /search/multi} de TMDb.
 * <p>
 * TMDb unifica películas y series en una sola respuesta con {@code media_type}:
 * <ul>
 *   <li>{@code "movie"} → usa {@code title} y {@code release_date}</li>
 *   <li>{@code "tv"}    → usa {@code name}  y {@code first_air_date}</li>
 * </ul>
 * Usar {@link #getDisplayTitle()} y {@link #getDisplayYear()} para obtener
 * el valor correcto independientemente del tipo.
 * </p>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TMDBMultiResultDTO {

    /** ID interno de TMDb. */
    private Long id;

    /** Tipo de resultado: {@code "movie"}, {@code "tv"} o {@code "person"}. */
    @JsonProperty("media_type")
    private String mediaType;

    /** Título de película. Null para series. */
    private String title;

    /** Título de serie. Null para películas. */
    private String name;

    /** Fecha de estreno de película (formato "YYYY-MM-DD"). Null para series. */
    @JsonProperty("release_date")
    private String releaseDate;

    /** Fecha de emisión del primer episodio (formato "YYYY-MM-DD"). Null para películas. */
    @JsonProperty("first_air_date")
    private String firstAirDate;

    /**
     * Devuelve el título correcto según el tipo de media.
     * Película → {@code title}, serie → {@code name}.
     */
    public String getDisplayTitle() {
        return title != null ? title : name;
    }

    /**
     * Devuelve el año de estreno como string de 4 dígitos, o {@code null} si no está disponible.
     * Película → primeros 4 chars de {@code release_date}, serie → primeros 4 chars de {@code first_air_date}.
     */
    public String getDisplayYear() {
        String date = releaseDate != null ? releaseDate : firstAirDate;
        return (date != null && date.length() >= 4) ? date.substring(0, 4) : null;
    }
}
