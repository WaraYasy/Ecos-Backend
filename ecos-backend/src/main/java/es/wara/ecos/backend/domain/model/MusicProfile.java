package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MusicProfile {

    private Long userId;
    private BigDecimal avgEnergy;
    private BigDecimal harmonicRichness;
    private BigDecimal eclecticity;
    private Map<String, Double> favoriteEras;
    private List<String> topGenres;

}