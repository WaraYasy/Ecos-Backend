package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Representa una película del catálogo de TMDB, usada para contextualizar
 * recomendaciones musicales por banda sonora o época.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Movie {

    private Long id;
    private Integer tmdbId;
    private String title;
    private Integer year;
    private String director;
    /** Géneros cinematográficos de la película (fuente: TMDb). */
    private List<String> genres;

}
