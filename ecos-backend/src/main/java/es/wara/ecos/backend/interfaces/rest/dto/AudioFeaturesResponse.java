package es.wara.ecos.backend.interfaces.rest.dto;

import es.wara.ecos.backend.domain.model.AudioFeatures;

import java.math.BigDecimal;

/**
 * DTO de respuesta para las características de audio de una canción.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public record AudioFeaturesResponse(
        BigDecimal energy,
        BigDecimal tempo,
        BigDecimal valence,
        BigDecimal danceability,
        BigDecimal acousticness,
        BigDecimal instrumentalness,
        BigDecimal speechiness,
        BigDecimal loudness,
        Integer musicalKey,
        Integer mode,
        Integer timeSignature
) {

    public static AudioFeaturesResponse from(AudioFeatures af) {
        if (af == null) return null;
        return new AudioFeaturesResponse(
                af.getEnergy(),
                af.getTempo(),
                af.getValence(),
                af.getDanceability(),
                af.getAcousticness(),
                af.getInstrumentalness(),
                af.getSpeechiness(),
                af.getLoudness(),
                af.getMusicalKey(),
                af.getMode(),
                af.getTimeSignature()
        );
    }

}
