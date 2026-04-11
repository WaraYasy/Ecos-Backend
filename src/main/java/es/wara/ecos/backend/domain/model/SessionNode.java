package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Canción dentro de una sesión de exploración, con su posición en el recorrido.
 * Las coordenadas visuales (x, y) ya no se persisten — el frontend recalcula el layout.
 *
 * @author Wara Pacheco
 * @version 2.0
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SessionNode {

    private UUID id;
    private UUID sessionId;
    private Song song;
    /** Orden de visita dentro de la sesión, comenzando en 1. */
    private Integer sequenceOrder;

}
