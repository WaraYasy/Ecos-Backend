package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Representa una película del catálogo de TMDB, usada para contextualizar
 * recomendaciones musicales por banda sonora o época.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Movie {

    private UUID id;
    private Integer tmdbId;
    private String title;
    private Integer year;
    private String director;
    /** Géneros cinematográficos de la película (fuente: TMDb). */
    private List<String> genres;

}
