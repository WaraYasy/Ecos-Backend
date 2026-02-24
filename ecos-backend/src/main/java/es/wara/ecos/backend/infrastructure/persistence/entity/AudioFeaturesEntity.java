package es.wara.ecos.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entidad JPA que mapea la tabla audio_features, con relación 1:1 con songs.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Entity
@Table(name = "audio_features")
@Getter
@Setter
@NoArgsConstructor
public class AudioFeaturesEntity {

    @Id
    @Column(name = "song_id")
    private Long songId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "song_id")
    private SongEntity song;

    @Column(precision = 4, scale = 3)
    private BigDecimal energy;

    @Column(precision = 6, scale = 3)
    private BigDecimal tempo;

    @Column(precision = 4, scale = 3)
    private BigDecimal valence;

    @Column(precision = 4, scale = 3)
    private BigDecimal danceability;

    @Column(precision = 4, scale = 3)
    private BigDecimal acousticness;

    @Column(precision = 4, scale = 3)
    private BigDecimal instrumentalness;

    @Column(precision = 4, scale = 3)
    private BigDecimal speechiness;

    @Column(precision = 6, scale = 3)
    private BigDecimal loudness;

    @Column(name = "musical_key")
    private Short musicalKey;

    @Column
    private Short mode;

    @Column(name = "time_signature")
    private Short timeSignature;

}
