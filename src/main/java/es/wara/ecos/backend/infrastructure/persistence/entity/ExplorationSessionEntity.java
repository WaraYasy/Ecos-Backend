package es.wara.ecos.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad JPA que mapea la tabla {@code sessions}.
 * <p>
 * Representa una sesión de exploración del grafo neuronal: el recorrido que
 * un usuario realiza partiendo de una canción raíz y navegando por canciones
 * relacionadas. Cada sesión contiene una lista ordenada de nodos
 * ({@link SessionNodeEntity}) con las canciones visitadas.
 * </p>
 * <p>
 * La colección {@code nodes} usa {@code @BatchSize(50)} para evitar el
 * problema N+1 al cargar múltiples sesiones: Hibernate agrupa las cargas
 * en bloques de 50 con {@code WHERE session_id IN (...)}.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.1
 * @since 1.0.0
 */
@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
public class ExplorationSessionEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_song_id")
    private SongEntity startSong;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceOrder ASC")
    private List<SessionNodeEntity> nodes = new ArrayList<>();

}