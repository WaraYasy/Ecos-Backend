package es.wara.ecos.backend.infrastructure.adapter.deezer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Representa un track devuelto por la Deezer Search API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeezerTrackDTO {

    private Long id;
    private String title;
    /** Duración en segundos. */
    private Integer duration;
    /** URL de preview MP3 de 30 segundos (siempre presente en Deezer). */
    private String preview;
    private String isrc;
    private DeezerArtistDTO artist;
    private DeezerAlbumDTO album;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }

    public String getIsrc() { return isrc; }
    public void setIsrc(String isrc) { this.isrc = isrc; }

    public DeezerArtistDTO getArtist() { return artist; }
    public void setArtist(DeezerArtistDTO artist) { this.artist = artist; }

    public DeezerAlbumDTO getAlbum() { return album; }
    public void setAlbum(DeezerAlbumDTO album) { this.album = album; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeezerArtistDTO {
        private Long id;
        private String name;
        @JsonProperty("picture_medium")
        private String pictureMedium;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPictureMedium() { return pictureMedium; }
        public void setPictureMedium(String pictureMedium) { this.pictureMedium = pictureMedium; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DeezerAlbumDTO {
        private Long id;
        private String title;
        @JsonProperty("cover_medium")
        private String coverMedium;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getCoverMedium() { return coverMedium; }
        public void setCoverMedium(String coverMedium) { this.coverMedium = coverMedium; }
    }
}
