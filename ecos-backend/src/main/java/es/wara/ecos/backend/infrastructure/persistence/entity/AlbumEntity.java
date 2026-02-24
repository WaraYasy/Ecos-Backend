package es.wara.ecos.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "albums")
@Getter
@Setter
@NoArgsConstructor
public class AlbumEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spotify_id", unique = true, nullable = false, length = 50)
    private String spotifyId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column
    private Short year;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id")
    private ArtistEntity artist;

}