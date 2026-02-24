package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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