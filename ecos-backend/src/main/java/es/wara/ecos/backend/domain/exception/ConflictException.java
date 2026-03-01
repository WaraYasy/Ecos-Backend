package es.wara.ecos.backend.domain.exception;

/**
 * Excepción de dominio lanzada cuando una operación entra en conflicto con el estado actual
 * del sistema (por ejemplo, recurso duplicado).
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

}
