package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Representa una sesión de exploración musical de un usuario, compuesta por
 * los nodos del grafo que fue visitando a partir de una canción inicial.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExplorationSession {

    private Long id;
    private Long userId;
    private Long startSongId;
    private LocalDateTime createdAt;
    private List<SessionNode> nodes;

}
