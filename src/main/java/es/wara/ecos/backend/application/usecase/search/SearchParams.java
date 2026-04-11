package es.wara.ecos.backend.application.usecase.search;

import java.math.BigDecimal;

/**
 * Parámetros de búsqueda multidimensional para el sistema de cuadrantes de ECOS.
 *
 * <p>Todos los campos son opcionales salvo {@code pageSize} y {@code page}.
 * Los cuadrantes activos son los que tienen valor no nulo:</p>
 * <ul>
 *   <li><b>Texto libre:</b> {@code q}</li>
 *   <li><b>Artista:</b> {@code artist} (ej. "Radiohead", "Björk")</li>
 *   <li><b>Estética:</b> {@code aesthetic}</li>
 *   <li><b>Género musical:</b> {@code genre} (ej. "rock", "jazz", "indie")</li>
 *   <li><b>Temporal:</b> {@code decade} (ej. "1980s") o {@code year} (ej. "1985", "1980-1990")</li>
 *   <li><b>Cinematográfico:</b> {@code movie}</li>
 *   <li><b>Geográfico:</b> {@code country} (ej. "Spain", "Japan", "United Kingdom")</li>
 *   <li><b>Técnico:</b> {@code instrumental}, {@code texture}, {@code mode},
 *       rangos de energía, valencia y BPM</li>
 * </ul>
 *
 * @author Wara Pacheco
 * @version 3.1
 * @since 1.0.0
 */
public record SearchParams(
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
        BigDecimal energyMin, BigDecimal energyMax,
        BigDecimal valenceMin, BigDecimal valenceMax,
        BigDecimal bpmMin,    BigDecimal bpmMax,
        int pageSize,
        int page,
        boolean enhanced
) {

    /** pageSize por defecto 20, máximo 50. page por defecto 0. */
    public SearchParams {
        if (pageSize <= 0) pageSize = 20;
        if (pageSize > 50) pageSize = 50;
        if (page < 0) page = 0;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String q, artist, aesthetic, genre, decade, year, movie, country, texture, mode;
        private Boolean instrumental;
        private BigDecimal energyMin, energyMax, valenceMin, valenceMax, bpmMin, bpmMax;
        private int pageSize = 20;
        private int page = 0;
        private boolean enhanced = false;

        public Builder q(String v)           { this.q = v;           return this; }
        public Builder artist(String v)      { this.artist = v;      return this; }
        public Builder aesthetic(String v)   { this.aesthetic = v;   return this; }
        public Builder genre(String v)       { this.genre = v;       return this; }
        public Builder decade(String v)      { this.decade = v;      return this; }
        public Builder year(String v)        { this.year = v;        return this; }
        public Builder movie(String v)       { this.movie = v;       return this; }
        public Builder country(String v)     { this.country = v;     return this; }
        public Builder texture(String v)     { this.texture = v;     return this; }
        public Builder pageSize(int v)       { this.pageSize = v;    return this; }
        public Builder page(int v)           { this.page = v;        return this; }
        public Builder enhanced(boolean v)   { this.enhanced = v;    return this; }

        public SearchParams build() {
            return new SearchParams(q, artist, aesthetic, genre, decade, year, movie, country,
                    instrumental, texture, mode,
                    energyMin, energyMax, valenceMin, valenceMax, bpmMin, bpmMax,
                    pageSize, page, enhanced);
        }
    }
}