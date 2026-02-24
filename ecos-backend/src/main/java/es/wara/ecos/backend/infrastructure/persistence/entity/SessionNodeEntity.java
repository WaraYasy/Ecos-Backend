package es.wara.ecos.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA que mapea la tabla session_nodes.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Entity
@Table(name = "session_nodes")
@Getter
@Setter
@NoArgsConstructor
public class SessionNodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ExplorationSessionEntity session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id")
    private SongEntity song;

    @Column(name = "x_position", precision = 8, scale = 4)
    private BigDecimal xPosition;

    @Column(name = "y_position", precision = 8, scale = 4)
    private BigDecimal yPosition;

    @Column(name = "visited_at")
    private LocalDateTime visitedAt;

    @Column(name = "sequence_order", nullable = false)
    private Short sequenceOrder;

}
