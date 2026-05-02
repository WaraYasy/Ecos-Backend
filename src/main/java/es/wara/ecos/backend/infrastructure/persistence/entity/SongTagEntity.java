package es.wara.ecos.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entidad JPA que mapea la tabla song_tags.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Entity
@Table(name = "song_tags")
@Getter
@Setter
@NoArgsConstructor
public class SongTagEntity {

    @EmbeddedId
    private SongTagId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("songId")
    @JoinColumn(name = "song_id")
    private SongEntity song;

    @Column(precision = 5, scale = 3)
    private BigDecimal weight;

    @Column(length = 20, nullable = false)
    private String type = "other";

}
