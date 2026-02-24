package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GraphNode {

    private Song song;
    private BigDecimal xPosition;
    private BigDecimal yPosition;
    private List<Song> connections;

}