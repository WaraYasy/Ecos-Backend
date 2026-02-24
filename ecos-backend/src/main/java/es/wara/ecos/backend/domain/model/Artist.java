package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa un artista musical en el dominio de ECOS.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Artist {

    private Long id;
    private String spotifyId;
    private String name;
    private String country;

}