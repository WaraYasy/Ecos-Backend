package es.wara.ecos.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_interactions")
@Getter
@Setter
@NoArgsConstructor
public class UserInteractionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id")
    private SongEntity song;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

}