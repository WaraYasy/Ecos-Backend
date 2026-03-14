package es.wara.ecos.backend.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

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

}
