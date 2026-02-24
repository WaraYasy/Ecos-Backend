package es.wara.ecos.backend.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SongTagId implements Serializable {

    @Column(name = "song_id")
    private Long songId;

    @Column(name = "tag", length = 100, nullable = false)
    private String tag;

}