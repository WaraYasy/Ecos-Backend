package es.wara.ecos.backend.interfaces.rest.handler;

import es.wara.ecos.backend.domain.exception.ConflictException;
import es.wara.ecos.backend.domain.exception.ForbiddenException;
import es.wara.ecos.backend.infrastructure.exception.ApiRateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manejador global de excepciones para la API REST de ECOS.
 *
 * <p>Intercepta excepciones de todas las capas y las transforma en respuestas HTTP
 * con un cuerpo JSON uniforme {@code {error, message, status}}:</p>
 *
 * <table>
 *   <tr><th>Excepción</th><th>HTTP</th><th>Cuándo</th></tr>
 *   <tr><td>{@link es.wara.ecos.backend.domain.exception.ConflictException}</td><td>409</td><td>Recurso duplicado (ej. email ya registrado)</td></tr>
 *   <tr><td>{@link es.wara.ecos.backend.domain.exception.ForbiddenException}</td><td>403</td><td>El usuario no es propietario del recurso (IDOR)</td></tr>
 *   <tr><td>{@link IllegalStateException}</td><td>401</td><td>Credenciales incorrectas en login</td></tr>
 *   <tr><td>{@link IllegalArgumentException}</td><td>404</td><td>Recurso no encontrado por ID</td></tr>
 *   <tr><td>{@link org.springframework.web.bind.MissingServletRequestParameterException}</td><td>400</td><td>Parámetro requerido ausente</td></tr>
 *   <tr><td>{@link org.springframework.web.bind.MethodArgumentNotValidException}</td><td>400</td><td>Validación de bean fallida (@Valid)</td></tr>
 *   <tr><td>{@link org.springframework.http.converter.HttpMessageNotReadableException}</td><td>400</td><td>JSON malformado o tipo incorrecto</td></tr>
 *   <tr><td>{@link UnsupportedOperationException}</td><td>501</td><td>Funcionalidad no implementada (stub)</td></tr>
 *   <tr><td>{@link es.wara.ecos.backend.infrastructure.exception.ApiRateLimitException}</td><td>429</td><td>Rate limit de una API externa</td></tr>
 *   <tr><td>{@link Exception}</td><td>500</td><td>Error inesperado (mensaje sanitizado, detalle en log)</td></tr>
 * </table>
 *
 * @author Wara Pacheco
 * @version 1.1
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    record ErrorResponse(String error, String message, int status) {}

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("CONFLICT", ex.getMessage(), 409));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("FORBIDDEN", ex.getMessage(), 403));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("UNAUTHORIZED", "Autenticación requerida", 401));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", ex.getMessage(), 404));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", "Parámetro requerido ausente: " + ex.getParameterName(), 400));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Error de validación");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", message, 400));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", "JSON malformado o tipo de dato incorrecto", 400));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorResponse> handleNotImplemented(UnsupportedOperationException ex) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(new ErrorResponse("NOT_IMPLEMENTED", ex.getMessage(), 501));
    }

    @ExceptionHandler(ApiRateLimitException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(ApiRateLimitException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorResponse("RATE_LIMIT", ex.getMessage(), 429));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Error inesperado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "Error interno del servidor", 500));
    }

}
