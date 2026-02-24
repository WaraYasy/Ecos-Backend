package es.wara.ecos.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA que mapea la tabla artists.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Entity
@Table(name = "artists")
@Getter
@Setter
@NoArgsConstructor
public class ArtistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spotify_id", unique = true, nullable = false, length = 50)
    private String spotifyId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 100)
    private String country;

}
