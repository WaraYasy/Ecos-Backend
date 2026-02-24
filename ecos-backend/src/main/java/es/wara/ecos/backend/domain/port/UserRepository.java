package es.wara.ecos.backend.domain.port;

import es.wara.ecos.backend.domain.model.User;

import java.util.Optional;

/**
 * Puerto de salida para el acceso y persistencia de usuarios en el dominio de ECOS.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public interface UserRepository {

    /**
     * Busca un usuario por su identificador interno.
     *
     * @param id identificador interno del usuario
     * @return el usuario encontrado, o vacío si no existe
     */
    Optional<User> findById(Long id);

    /**
     * Busca un usuario por su nombre de usuario.
     *
     * @param username nombre de usuario único
     * @return el usuario encontrado, o vacío si no existe
     */
    Optional<User> findByUsername(String username);

    /**
     * Busca un usuario por su dirección de correo electrónico.
     *
     * @param email dirección de correo electrónico del usuario
     * @return el usuario encontrado, o vacío si no existe
     */
    Optional<User> findByEmail(String email);

    /**
     * Persiste un usuario en el repositorio.
     *
     * @param user usuario a guardar
     * @return el usuario guardado con su identificador asignado
     */
    User save(User user);

}
