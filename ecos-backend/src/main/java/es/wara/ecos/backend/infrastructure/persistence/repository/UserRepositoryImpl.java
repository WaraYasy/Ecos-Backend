package es.wara.ecos.backend.infrastructure.persistence.repository;

import es.wara.ecos.backend.domain.model.User;
import es.wara.ecos.backend.domain.port.UserRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaUserRepository;
import es.wara.ecos.backend.infrastructure.persistence.mapper.UserMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final JpaUserRepository jpa;
    private final UserMapper mapper;

    public UserRepositoryImpl(JpaUserRepository jpa, UserMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpa.findById(id).map(mapper::toModel);
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
    public User save(User user) {
        return mapper.toModel(jpa.save(mapper.toEntity(user)));
    }

}