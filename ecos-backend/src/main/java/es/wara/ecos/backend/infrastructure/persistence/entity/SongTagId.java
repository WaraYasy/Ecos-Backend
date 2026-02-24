package es.wara.ecos.backend.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Clave primaria compuesta (song_id, tag) para la tabla song_tags.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
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
