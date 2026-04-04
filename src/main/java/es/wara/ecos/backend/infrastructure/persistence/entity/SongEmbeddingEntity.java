package es.wara.ecos.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA que mapea la tabla song_embeddings.
 * El campo embedding se almacena como String en formato "[v1,v2,...,v384]",
 * que PostgreSQL convierte automáticamente al tipo vector mediante sus funciones I/O.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Entity
@Table(name = "song_embeddings")
@Getter
@Setter
@NoArgsConstructor
public class SongEmbeddingEntity {

    @Id
    @Column(name = "song_id")
    private UUID songId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "song_id")
    private SongEntity song;

    /** Vector pgvector de 384 dimensiones. Se serializa como "[v1,v2,...,v384]". */
    @Column(columnDefinition = "vector(384)", nullable = false)
    private String embedding;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}