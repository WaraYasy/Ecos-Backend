package es.wara.ecos.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

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

}