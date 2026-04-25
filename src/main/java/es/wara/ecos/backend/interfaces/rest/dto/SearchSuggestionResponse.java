package es.wara.ecos.backend.interfaces.rest.dto;

import es.wara.ecos.backend.config.SearchSuggestionsProperties.SuggestionItem;
import es.wara.ecos.backend.config.SearchSuggestionsProperties.SuggestionParams;

import java.math.BigDecimal;

/**
 * DTO de respuesta para una sugerencia de búsqueda predefinida.
 *
 * <p>Cada sugerencia lleva el texto visible ({@code label}) y los parámetros
 * de búsqueda ya construidos ({@code params}), listos para enviarse directamente
 * a {@code GET /v1/search}.</p>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
public record SearchSuggestionResponse(
        String label,
        Params params
) {

    public static SearchSuggestionResponse from(SuggestionItem item) {
        SuggestionParams p = item.getParams();
        return new SearchSuggestionResponse(
                item.getLabel(),
                new Params(
                        p.getQ(), p.getArtist(), p.getAesthetic(), p.getGenre(),
                        p.getDecade(), p.getYear(), p.getMovie(), p.getCountry(),
                        p.getInstrumental(), p.getTexture(), p.getMode(),
                        p.getEnergyMin(), p.getEnergyMax(),
                        p.getValenceMin(), p.getValenceMax(),
                        p.getBpmMin(), p.getBpmMax(),
                        p.getEnhanced()
                )
        );
    }

    public record Params(
            String q,
            String artist,
            String aesthetic,
            String genre,
            String decade,
            String year,
            String movie,
            String country,
            Boolean instrumental,
            String texture,
            String mode,
            BigDecimal energyMin,
            BigDecimal energyMax,
            BigDecimal valenceMin,
            BigDecimal valenceMax,
            BigDecimal bpmMin,
            BigDecimal bpmMax,
            Boolean enhanced
    ) {}
}