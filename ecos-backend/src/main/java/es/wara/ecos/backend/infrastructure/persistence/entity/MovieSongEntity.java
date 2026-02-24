package es.wara.ecos.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Table(name = "song_movies")
@Getter
@Setter
@NoArgsConstructor
public class MovieSongEntity {

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class MovieSongId implements Serializable {
        @Column(name = "song_id")
        private Long songId;

        @Column(name = "movie_id")
        private Long movieId;
    }

    @EmbeddedId
    private MovieSongId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("songId")
    @JoinColumn(name = "song_id")
    private SongEntity song;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("movieId")
    @JoinColumn(name = "movie_id")
    private MovieEntity movie;

}