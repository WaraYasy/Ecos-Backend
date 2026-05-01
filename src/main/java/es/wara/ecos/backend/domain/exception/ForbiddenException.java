package es.wara.ecos.backend.domain.exception;

/**
 * Excepción de dominio lanzada cuando un usuario intenta realizar una operación
 * sobre un recurso que no le pertenece.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public class ForbiddenException extends RuntimeException {

    /**
     * Crea una excepción de acceso denegado con el mensaje descriptivo del recurso protegido.
     *
     * @param message descripción del acceso denegado (ej. "No tienes permiso para eliminar esta playlist")
     */
    public ForbiddenException(String message) {
        super(message);
    }

}
