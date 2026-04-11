package es.wara.ecos.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * Entidad JPA que mapea la tabla session_songs.
 * Representa una canción dentro de una sesión de exploración guardada.
 *
 * @author Wara Pacheco
 * @version 2.0
 * @since 1.0.0
 */
@Entity
@Table(name = "session_songs")
@Getter
@Setter
@NoArgsConstructor
public class SessionNodeEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ExplorationSessionEntity session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id")
    private SongEntity song;

    @Column(name = "sequence_order", nullable = false)
    private Short sequenceOrder;

}