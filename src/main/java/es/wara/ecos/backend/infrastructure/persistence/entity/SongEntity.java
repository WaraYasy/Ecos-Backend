package es.wara.ecos.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad JPA que mapea la tabla {@code songs}.
 * <p>
 * Agrega las relaciones principales de una canción: álbum, artistas
 * ({@code song_artists}), características de audio ({@code audio_features}),
 * etiquetas de género ({@code song_tags}) y películas asociadas
 * ({@code song_movies}).
 * </p>
 * <p>
 * Estrategia de carga lazy con batch para evitar N+1:
 * <ul>
 *   <li>{@code @BatchSize(50)} a nivel de clase → carga en batch cuando
 *       múltiples canciones se resuelven desde relaciones {@code @ManyToOne}
 *       (p. ej. nodos de sesión).</li>
 *   <li>{@code @BatchSize(25)} en {@code artists}, {@code tags} y
 *       {@code movies} → sus colecciones se agrupan en bloques de 25.</li>
 * </ul>
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.1
 * @since 1.0.0
 */
@BatchSize(size = 50)
@Entity
@Table(name = "songs")
@Getter
@Setter
@NoArgsConstructor
public class SongEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "spotify_id", length = 50)
    private String spotifyId;

    @Column(length = 15, unique = true)
    private String isrc;

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
    @BatchSize(size = 25)
    private List<ArtistEntity> artists = new ArrayList<>();

    @OneToOne(mappedBy = "song", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private AudioFeaturesEntity audioFeatures;

    @OneToMany(mappedBy = "song", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 25)
    private List<SongTagEntity> tags = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "song_movies",
            joinColumns = @JoinColumn(name = "song_id"),
            inverseJoinColumns = @JoinColumn(name = "movie_id")
    )
    @BatchSize(size = 25)
    private List<MovieEntity> movies = new ArrayList<>();

}