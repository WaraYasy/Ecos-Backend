package es.wara.ecos.backend.domain.port;

import es.wara.ecos.backend.domain.model.ExplorationSession;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida para el acceso y persistencia de sesiones de exploración
 * en el dominio de ECOS.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public interface SessionRepository {

    /**
     * Busca una sesión de exploración por su identificador interno.
     *
     * @param id identificador interno de la sesión
     * @return la sesión encontrada, o vacío si no existe
     */
    Optional<ExplorationSession> findById(Long id);

    /**
     * Obtiene el historial de sesiones de exploración de un usuario.
     *
     * @param userId identificador del usuario
     * @return lista de sesiones del usuario ordenadas cronológicamente
     */
    List<ExplorationSession> findByUserId(Long userId);

    /**
     * Persiste una sesión de exploración en el repositorio.
     *
     * @param session sesión a guardar
     * @return la sesión guardada con su identificador asignado
     */
    ExplorationSession save(ExplorationSession session);

}
