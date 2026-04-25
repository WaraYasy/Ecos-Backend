package es.wara.ecos.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Sugerencias de búsqueda predefinidas que el frontend puede mostrar al usuario.
 *
 * <p>Se cargan desde {@code suggestions.yml} (incluido en el JAR como valores por defecto)
 * y pueden sobreescribirse con un fichero externo montado en {@code /config/suggestions.yml}.</p>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "suggestions")
public class SearchSuggestionsProperties {

    private List<SuggestionItem> items = new ArrayList<>();

    public List<SuggestionItem> getItems() { return items; }
    public void setItems(List<SuggestionItem> items) { this.items = items; }

    public static class SuggestionItem {

        private String label;
        private SuggestionParams params = new SuggestionParams();

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public SuggestionParams getParams() { return params; }
        public void setParams(SuggestionParams params) { this.params = params; }
    }

    public static class SuggestionParams {

        private String q;
        private String artist;
        private String aesthetic;
        private String genre;
        private String decade;
        private String year;
        private String movie;
        private String country;
        private Boolean instrumental;
        private String texture;
        private String mode;
        private BigDecimal energyMin;
        private BigDecimal energyMax;
        private BigDecimal valenceMin;
        private BigDecimal valenceMax;
        private BigDecimal bpmMin;
        private BigDecimal bpmMax;
        private Boolean enhanced;

        public String getQ() { return q; }
        public void setQ(String q) { this.q = q; }

        public String getArtist() { return artist; }
        public void setArtist(String artist) { this.artist = artist; }

        public String getAesthetic() { return aesthetic; }
        public void setAesthetic(String aesthetic) { this.aesthetic = aesthetic; }

        public String getGenre() { return genre; }
        public void setGenre(String genre) { this.genre = genre; }

        public String getDecade() { return decade; }
        public void setDecade(String decade) { this.decade = decade; }

        public String getYear() { return year; }
        public void setYear(String year) { this.year = year; }

        public String getMovie() { return movie; }
        public void setMovie(String movie) { this.movie = movie; }

        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }

        public Boolean getInstrumental() { return instrumental; }
        public void setInstrumental(Boolean instrumental) { this.instrumental = instrumental; }

        public String getTexture() { return texture; }
        public void setTexture(String texture) { this.texture = texture; }

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }

        public BigDecimal getEnergyMin() { return energyMin; }
        public void setEnergyMin(BigDecimal energyMin) { this.energyMin = energyMin; }

        public BigDecimal getEnergyMax() { return energyMax; }
        public void setEnergyMax(BigDecimal energyMax) { this.energyMax = energyMax; }

        public BigDecimal getValenceMin() { return valenceMin; }
        public void setValenceMin(BigDecimal valenceMin) { this.valenceMin = valenceMin; }

        public BigDecimal getValenceMax() { return valenceMax; }
        public void setValenceMax(BigDecimal valenceMax) { this.valenceMax = valenceMax; }

        public BigDecimal getBpmMin() { return bpmMin; }
        public void setBpmMin(BigDecimal bpmMin) { this.bpmMin = bpmMin; }

        public BigDecimal getBpmMax() { return bpmMax; }
        public void setBpmMax(BigDecimal bpmMax) { this.bpmMax = bpmMax; }

        public Boolean getEnhanced() { return enhanced; }
        public void setEnhanced(Boolean enhanced) { this.enhanced = enhanced; }
    }
}