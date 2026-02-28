package es.wara.ecos.backend.interfaces.rest.dto;

import es.wara.ecos.backend.domain.model.MusicProfile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTO de respuesta con el perfil musical calculado de un usuario.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public record MusicProfileResponse(
        Long userId,
        BigDecimal avgEnergy,
        BigDecimal harmonicRichness,
        BigDecimal eclecticity,
        Map<String, Double> favoriteEras,
        List<String> topGenres
) {

    public static MusicProfileResponse from(MusicProfile profile) {
        return new MusicProfileResponse(
                profile.getUserId(),
                profile.getAvgEnergy(),
                profile.getHarmonicRichness(),
                profile.getEclecticity(),
                profile.getFavoriteEras(),
                profile.getTopGenres()
        );
    }

}
