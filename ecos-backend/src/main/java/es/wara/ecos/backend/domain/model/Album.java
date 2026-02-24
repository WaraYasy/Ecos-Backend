package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa un álbum musical asociado a un artista en el dominio de ECOS.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Album {

    private Long id;
    private String spotifyId;
    private String title;
    private Integer year;
    private String coverUrl;
    private Long artistId;

}
