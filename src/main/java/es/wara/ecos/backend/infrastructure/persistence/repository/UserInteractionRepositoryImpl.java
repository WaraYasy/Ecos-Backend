package es.wara.ecos.backend.infrastructure.persistence.repository;

import es.wara.ecos.backend.infrastructure.persistence.entity.UserInteractionEntity;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaUserInteractionRepository;
import org.springframework.stereotype.Repository;

/**
 * Componente interno de infraestructura para persistir interacciones de usuario en la tabla user_interactions.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Repository
public class UserInteractionRepositoryImpl {

    private final JpaUserInteractionRepository jpa;

    public UserInteractionRepositoryImpl(JpaUserInteractionRepository jpa) {
        this.jpa = jpa;
    }

    public UserInteractionEntity save(UserInteractionEntity interaction) {
        return jpa.save(interaction);
    }

}
