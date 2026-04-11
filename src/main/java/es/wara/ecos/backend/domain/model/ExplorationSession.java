package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Sesión de exploración musical guardada por el usuario.
 * Contiene la lista ordenada de canciones visitadas en el grafo.
 * Cada usuario puede tener un máximo de 2 sesiones guardadas.
 *
 * @author Wara Pacheco
 * @version 2.0
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExplorationSession {

    private UUID id;
    private UUID userId;
    private UUID startSongId;
    private String name;
    private LocalDateTime createdAt;
    private List<SessionNode> nodes;

}
