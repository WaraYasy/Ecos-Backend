package es.wara.ecos.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

/**
 * Entidad JPA que mapea la tabla {@code session_songs}.
 * <p>
 * Representa un nodo del grafo de exploración: una canción visitada durante
 * una sesión, con su posición en el recorrido ({@code sequenceOrder}).
 * El nombre "node" refleja su rol en el grafo; la tabla se llama
 * {@code session_songs} por razones históricas.
 * </p>
 * <p>
 * Ambas relaciones {@code @ManyToOne} son LAZY. La carga en batch de
 * {@link SongEntity} está configurada con {@code @BatchSize(50)} a nivel
 * de clase en {@link SongEntity}, por lo que Hibernate agrupa las cargas
 * con {@code WHERE id IN (...)} al materializar múltiples nodos.
 * </p>
 *
 * @author Wara Pacheco
 * @version 2.1
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