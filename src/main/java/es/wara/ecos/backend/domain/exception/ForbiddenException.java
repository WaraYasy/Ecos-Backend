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

    public ForbiddenException(String message) {
        super(message);
    }

}
