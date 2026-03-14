package es.wara.ecos.backend.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidad JPA que mapea la tabla user_profiles, con clave primaria compartida con users (relación 1:1).
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
public class UserProfileEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "avg_energy", precision = 4, scale = 3)
    private BigDecimal avgEnergy;

    @Column(name = "harmonic_richness", precision = 4, scale = 3)
    private BigDecimal harmonicRichness;

    @Column(precision = 4, scale = 3)
    private BigDecimal eclecticity;

    @Column(name = "favorite_eras", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String favoriteEras;

    @Column(name = "top_genres", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String topGenres;

}