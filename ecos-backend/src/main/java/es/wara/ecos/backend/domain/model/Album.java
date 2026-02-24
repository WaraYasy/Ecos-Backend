package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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