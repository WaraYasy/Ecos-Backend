package es.wara.ecos.backend.interfaces.rest.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO de petición para actualizar los datos de un usuario de ECOS.
 *
 * <p>Todos los campos son opcionales — solo se actualizan los que lleguen con valor no nulo.
 * Semántica PATCH: los campos ausentes o nulos no modifican el valor existente.</p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public record UpdateUserRequest(
        @Size(min = 3, max = 50) String username,
        String language
) {}