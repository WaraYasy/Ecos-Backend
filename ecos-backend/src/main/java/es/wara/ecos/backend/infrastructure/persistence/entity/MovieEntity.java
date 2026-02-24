package es.wara.ecos.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA que mapea la tabla movies.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
public class MovieEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tmdb_id", unique = true, nullable = false)
    private Integer tmdbId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column
    private Short year;

    @Column(length = 255)
    private String director;

}
