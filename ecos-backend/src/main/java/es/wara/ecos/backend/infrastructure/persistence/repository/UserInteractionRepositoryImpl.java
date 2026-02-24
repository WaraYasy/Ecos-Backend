package es.wara.ecos.backend.infrastructure.persistence.repository;

import es.wara.ecos.backend.infrastructure.persistence.entity.UserInteractionEntity;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaUserInteractionRepository;
import org.springframework.stereotype.Repository;

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