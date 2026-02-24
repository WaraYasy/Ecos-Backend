package es.wara.ecos.backend.domain.port;

import es.wara.ecos.backend.domain.model.User;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    User save(User user);

}