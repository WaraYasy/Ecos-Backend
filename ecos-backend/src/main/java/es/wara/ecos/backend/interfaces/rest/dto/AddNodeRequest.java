package es.wara.ecos.backend.interfaces.rest.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO de petición para añadir un nodo a una sesión de exploración activa.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public record AddNodeRequest(
        @NotNull UUID songId,
        BigDecimal x,
        BigDecimal y
) {}
