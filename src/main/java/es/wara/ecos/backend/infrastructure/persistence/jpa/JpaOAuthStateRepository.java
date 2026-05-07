package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.OAuthStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

/**
 * Repositorio Spring Data JPA para OAuthStateEntity.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public interface JpaOAuthStateRepository extends JpaRepository<OAuthStateEntity, String> {

    /**
     * Elimina en bloque todos los estados OAuth expirados antes del instante dado.
     * Llamado por {@code OAuthStateCleanupScheduler} cada hora.
     *
     * @param cutoff instante de corte; se borran los registros con {@code expiresAt < cutoff}
     * @return número de registros eliminados
     */
    @Modifying
    @Query("DELETE FROM OAuthStateEntity s WHERE s.expiresAt < :cutoff")
    int deleteByExpiresAtBefore(@Param("cutoff") Instant cutoff);

}
