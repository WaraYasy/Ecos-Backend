package es.wara.ecos.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

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
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "spotify_id", unique = true, nullable = false, length = 50)
    private String spotifyId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 100)
    private String country;

    /** Géneros musicales del artista almacenados como array JSON (fuente: Spotify). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> genres;

}