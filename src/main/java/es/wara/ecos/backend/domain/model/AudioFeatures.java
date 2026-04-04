package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

import static java.lang.Math.max;

/**
 * Características de audio de una canción obtenidas de Spotify,
 * usadas para calcular similitud y construir el perfil musical del usuario.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AudioFeatures {

    private UUID songId;
    /** Intensidad y actividad percibida: 0.0 a 1.0. */
    private BigDecimal energy;
    /** Velocidad del tempo en BPM. */
    private BigDecimal tempo;
    /** Positividad emocional: 0.0 (triste) a 1.0 (eufórico). */
    private BigDecimal valence;
    /** Idoneidad para bailar: 0.0 a 1.0. */
    private BigDecimal danceability;
    /** Presencia de instrumentos acústicos: 0.0 a 1.0. */
    private BigDecimal acousticness;
    /** Ausencia de voz humana: 0.0 a 1.0. */
    private BigDecimal instrumentalness;
    /** Presencia de palabra hablada: 0.0 a 1.0. */
    private BigDecimal speechiness;
    /** Volumen medio en decibelios, típicamente entre -60 y 0 dB. */
    private BigDecimal loudness;
    /** Tonalidad musical: 0=C, 1=C#, 2=D, ... 11=B. */
    private Integer musicalKey;
    /** Modalidad: 0=menor, 1=mayor. */
    private Integer mode;
    private Integer timeSignature;

    /**
     * Convierte las características de audio en un vector normalizado de 8 dimensiones
     * para calcular similitud coseno. El tempo se normaliza a [0,1] asumiendo rango 40–220 BPM.
     * El loudness se normaliza de [-60, 0] dB a [0, 1].
     *
     * @return vector float[8]: [energy, tempo_norm, valence, danceability,
     *                           acousticness, instrumentalness, speechiness, loudness_norm]
     */
    public float[] toNormalizedVector() {
        float tempoNorm = tempo != null
                ? (float) Math.min(1.0, max(0.0, (tempo.doubleValue() - 40.0) / 180.0))
                : 0f;
        float loudnessNorm = loudness != null
                ? (float) Math.min(1.0, max(0.0, (loudness.doubleValue() + 60.0) / 60.0))
                : 0f;
        return new float[]{
                toFloat(energy),
                tempoNorm,
                toFloat(valence),
                toFloat(danceability),
                toFloat(acousticness),
                toFloat(instrumentalness),
                toFloat(speechiness),
                loudnessNorm
        };
    }

    private float toFloat(BigDecimal value) {
        return value != null ? value.floatValue() : 0f;
    }

}
