package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AudioFeatures {

    private Long songId;
    private BigDecimal energy;
    private BigDecimal tempo;
    private BigDecimal valence;
    private BigDecimal danceability;
    private BigDecimal acousticness;
    private BigDecimal instrumentalness;
    private BigDecimal speechiness;
    private BigDecimal loudness;
    private Integer musicalKey;
    private Integer mode;
    private Integer timeSignature;

}