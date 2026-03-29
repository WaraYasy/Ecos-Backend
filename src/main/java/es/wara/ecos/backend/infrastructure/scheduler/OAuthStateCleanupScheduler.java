package es.wara.ecos.backend.infrastructure.scheduler;

import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaOAuthStateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Limpia periódicamente los OAuth states expirados de la tabla {@code oauth_states}.
 *
 * <p>Se ejecuta cada hora. Los states tienen TTL de 10 minutos, por lo que
 * esta limpieza previene el crecimiento indefinido de la tabla.</p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Slf4j
@Component
public class OAuthStateCleanupScheduler {

    private final JpaOAuthStateRepository oAuthStateRepository;

    public OAuthStateCleanupScheduler(JpaOAuthStateRepository oAuthStateRepository) {
        this.oAuthStateRepository = oAuthStateRepository;
    }

    @Transactional
    @Scheduled(fixedRate = 3_600_000)
    public void cleanupExpiredStates() {
        int deleted = oAuthStateRepository.deleteByExpiresAtBefore(Instant.now());
        if (deleted > 0) {
            log.info("Limpiados {} OAuth states expirados", deleted);
        }
    }

}