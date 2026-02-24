package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionNode {

    private Long id;
    private Long sessionId;
    private Song song;
    private BigDecimal xPosition;
    private BigDecimal yPosition;
    private LocalDateTime visitedAt;
    private Integer sequenceOrder;

}