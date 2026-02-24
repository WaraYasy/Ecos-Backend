package es.wara.ecos.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "playlist_songs")
@Getter
@Setter
@NoArgsConstructor
public class PlaylistSongEntity {

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class PlaylistSongId implements Serializable {
        @Column(name = "playlist_id")
        private Long playlistId;

        @Column(name = "song_id")
        private Long songId;
    }

    @EmbeddedId
    private PlaylistSongId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("playlistId")
    @JoinColumn(name = "playlist_id")
    private PlaylistEntity playlist;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("songId")
    @JoinColumn(name = "song_id")
    private SongEntity song;

    @Column(nullable = false)
    private Short position;

}