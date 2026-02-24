package es.wara.ecos.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA que mapea la tabla songs, con relaciones a albums, artists (song_artists), audio_features y song_tags.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Entity
@Table(name = "songs")
@Getter
@Setter
@NoArgsConstructor
public class SongEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spotify_id", unique = true, nullable = false, length = 50)
    private String spotifyId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "preview_url", length = 500)
    private String previewUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id")
    private AlbumEntity album;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "song_artists",
            joinColumns = @JoinColumn(name = "song_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id")
    )
    private List<ArtistEntity> artists = new ArrayList<>();

    @OneToOne(mappedBy = "song", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private AudioFeaturesEntity audioFeatures;

    @OneToMany(mappedBy = "song", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SongTagEntity> tags = new ArrayList<>();

}
