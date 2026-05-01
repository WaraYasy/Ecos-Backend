package es.wara.ecos.backend.domain.port;

import es.wara.ecos.backend.domain.model.User;

import java.util.Optional;
import java.util.UUID;

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
    Optional<User> findById(UUID id);

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
     * Busca un usuario por su ID de proveedor OAuth externo (ej. ID de Spotify).
     *
     * @param providerId identificador único devuelto por el proveedor OAuth
     * @return el usuario encontrado, o vacío si no existe
     */
    Optional<User> findByProviderId(String providerId);

    /**
     * Busca un usuario por su código único de 5 caracteres alfanuméricos (ej. {@code ABCD1}).
     *
     * @param userCode código en mayúsculas
     * @return el usuario encontrado, o vacío si no existe
     */
    Optional<User> findByUserCode(String userCode);

    /**
     * Comprueba si existe un usuario con el código dado, usado para garantizar unicidad al generarlo.
     *
     * @param userCode código a comprobar
     * @return {@code true} si ya existe un usuario con ese código
     */
    boolean existsByUserCode(String userCode);

    /**
     * Comprueba si existe un usuario con el identificador dado sin cargar la entidad completa.
     *
     * @param id identificador interno del usuario
     * @return {@code true} si el usuario existe
     */
    boolean existsById(UUID id);

    /**
     * Persiste un usuario en el repositorio.
     *
     * @param user usuario a guardar
     * @return el usuario guardado con su identificador asignado
     */
    User save(User user);

}
