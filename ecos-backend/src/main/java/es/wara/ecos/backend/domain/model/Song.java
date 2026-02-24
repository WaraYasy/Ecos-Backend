package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Song {

    private Long id;
    private String spotifyId;
    private String title;
    private Integer durationMs;
    private String previewUrl;
    private Album album;
    private List<Artist> artists;
    private AudioFeatures audioFeatures;
    private List<Tag> tags;
    private LocalDateTime createdAt;

}