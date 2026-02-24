package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Playlist {

    private Long id;
    private Long userId;
    private String name;
    private String description;
    private String spotifyExportId;
    private LocalDateTime createdAt;
    private List<Song> songs;

}
