package es.wara.ecos.backend.infrastructure.persistence.repository;

import es.wara.ecos.backend.domain.model.User;
import es.wara.ecos.backend.domain.port.UserRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaUserRepository;
import es.wara.ecos.backend.infrastructure.persistence.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StopWatch;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementación del puerto de dominio UserRepository usando JPA.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Slf4j
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final JpaUserRepository jpa;
    private final UserMapper mapper;

    public UserRepositoryImpl(JpaUserRepository jpa, UserMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Optional<User> findById(UUID id) {
        StopWatch sw = new StopWatch();
        sw.start();
        Optional<User> result = jpa.findById(id).map(mapper::toModel);
        sw.stop();
        long ms = sw.getTotalTimeMillis();
        if (ms > 100) {
            log.warn("UserRepository.findById: consulta lenta id={} → {}ms", id, ms);
        } else {
            log.debug("UserRepository.findById: id={} → {}ms (encontrado={})", id, ms, result.isPresent());
        }
        return result;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jpa.findByUsername(username).map(mapper::toModel);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpa.findByEmail(email).map(mapper::toModel);
    }

    @Override
    public Optional<User> findByProviderId(String providerId) {
        return jpa.findByProviderId(providerId).map(mapper::toModel);
    }

    @Override
    public User save(User user) {
        return mapper.toModel(jpa.save(mapper.toEntity(user)));
    }

}
