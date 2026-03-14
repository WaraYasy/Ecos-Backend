package es.wara.ecos.backend.infrastructure.adapter.spotify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa las características de audio de un track en Spotify.
 * <p>
 * Se deserializa desde {@code GET /v1/audio-features/{id}}.
 * Los valores de 0.0 a 1.0 son proporciones normalizadas.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotifyAudioFeaturesDTO {

    /** Identificador del track en Spotify. */
    private String id;

    /** Intensidad y actividad percibida: 0.0 (tranquilo) a 1.0 (enérgico). */
    private Double energy;

    /** Velocidad estimada del tempo en BPM. */
    private Double tempo;

    /** Positividad musical: 0.0 (triste) a 1.0 (alegre/eufórico). */
    private Double valence;

    /** Idoneidad para bailar: 0.0 a 1.0. */
    private Double danceability;

    /** Presencia de instrumentos acústicos: 0.0 a 1.0. */
    private Double acousticness;

    /** Probabilidad de que el track sea instrumental (sin voz): 0.0 a 1.0. */
    private Double instrumentalness;

    /** Presencia de palabra hablada: 0.0 a 1.0. */
    private Double speechiness;

    /** Volumen medio en decibelios, normalmente entre -60 y 0 dB. */
    private Double loudness;

    /** Tonalidad musical: 0=Do, 1=Do#, 2=Re, …, 11=Si. -1 si no se detecta. */
    private Integer key;

    /** Modalidad: 0=menor, 1=mayor. */
    private Integer mode;

    /** Compás: número de tiempos por compás (normalmente 3 o 4). */
    @JsonProperty("time_signature")
    private Integer timeSignature;
}