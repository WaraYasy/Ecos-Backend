package es.wara.ecos.backend.domain.service;

import es.wara.ecos.backend.domain.model.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Servicio de dominio que calcula la similitud entre canciones usando la fórmula
 * multi-factor de ECOS:
 *
 * <pre>
 * similarity(A, B) =
 *   0.35 × audio_similarity     (coseno sobre 8 features de AudioFeatures)
 *   0.25 × semantic_similarity  (coseno sobre embeddings pgvector)
 *   0.20 × tag_similarity       (Jaccard sobre tags)
 *   0.10 × era_proximity        (decaimiento gaussiano por año)
 *   0.10 × movie_overlap        (Jaccard sobre películas)
 * </pre>
 *
 * Si algún componente no está disponible (ej. embeddings aún no generados), su peso
 * se redistribuye proporcionalmente entre los componentes disponibles.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class SimilarityService {

    // Desviación estándar para decaimiento gaussiano era (~15 años)
    private static final double ERA_SIGMA = 15.0;

    /**
     * Calcula la similitud total entre dos canciones.
     * Devuelve un valor entre 0.0 (nada similares) y 1.0 (idénticas).
     *
     * @param a           primera canción con sus features, tags y movies cargados
     * @param b           segunda canción
     * @param embeddingA  embedding semántico de A (puede ser null)
     * @param embeddingB  embedding semántico de B (puede ser null)
     * @return score de similitud [0.0, 1.0]
     */
    public float compute(Song a, Song b, float[] embeddingA, float[] embeddingB) {
        boolean hasEmbeddings = embeddingA != null && embeddingB != null;

        Integer yearA = a.getAlbum() != null ? a.getAlbum().getYear() : null;
        Integer yearB = b.getAlbum() != null ? b.getAlbum().getYear() : null;

        float audio    = audioSimilarity(a.getAudioFeatures(), b.getAudioFeatures());
        float semantic = hasEmbeddings ? semanticSimilarity(embeddingA, embeddingB) : 0f;
        float tag      = tagSimilarity(a.getTags(), b.getTags());
        float era      = eraProximity(yearA, yearB);
        float movie    = movieOverlap(a.getMovies(), b.getMovies());

        if (!hasEmbeddings) {
            // Redistribuir el 0.25 del componente semántico proporcionalmente
            return (0.35f + 0.25f * 0.35f / 0.75f) * audio
                    + (0.20f + 0.25f * 0.20f / 0.75f) * tag
                    + (0.10f + 0.25f * 0.10f / 0.75f) * era
                    + (0.10f + 0.25f * 0.10f / 0.75f) * movie;
        }

        return 0.35f * audio + 0.25f * semantic + 0.20f * tag + 0.10f * era + 0.10f * movie;
    }

    /**
     * Similitud de audio usando similitud coseno sobre el vector normalizado de 8 features.
     */
    public float audioSimilarity(AudioFeatures a, AudioFeatures b) {
        if (a == null || b == null) return 0f;
        float[] vecA = a.toNormalizedVector();
        float[] vecB = b.toNormalizedVector();
        return cosineSimilarity(vecA, vecB);
    }

    /**
     * Similitud semántica usando similitud coseno sobre embeddings de 384 dimensiones.
     */
    public float semanticSimilarity(float[] embeddingA, float[] embeddingB) {
        if (embeddingA == null || embeddingB == null) return 0f;
        return cosineSimilarity(embeddingA, embeddingB);
    }

    /**
     * Similitud de tags usando índice de Jaccard: |A ∩ B| / |A ∪ B|.
     */
    public float tagSimilarity(List<Tag> tagsA, List<Tag> tagsB) {
        if (tagsA == null || tagsB == null || tagsA.isEmpty() || tagsB.isEmpty()) return 0f;
        long intersection = tagsA.stream()
                .filter(t -> tagsB.stream().anyMatch(u -> u.getName().equalsIgnoreCase(t.getName())))
                .count();
        long union = tagsA.size() + tagsB.size() - intersection;
        return union == 0 ? 0f : (float) intersection / union;
    }

    /**
     * Proximidad temporal usando decaimiento gaussiano: exp(-Δyears² / (2σ²)).
     * σ=15 años → canciones del mismo año tienen similitud 1.0; a 30 años de distancia ~0.13.
     */
    public float eraProximity(Integer yearA, Integer yearB) {
        if (yearA == null || yearB == null) return 0f;
        double delta = yearA - yearB;
        return (float) Math.exp(-(delta * delta) / (2 * ERA_SIGMA * ERA_SIGMA));
    }

    /**
     * Similitud de películas usando índice de Jaccard sobre los títulos de las películas asociadas.
     */
    public float movieOverlap(List<Movie> moviesA, List<Movie> moviesB) {
        if (moviesA == null || moviesB == null || moviesA.isEmpty() || moviesB.isEmpty()) return 0f;
        long intersection = moviesA.stream()
                .filter(m -> moviesB.stream().anyMatch(n -> n.getId() != null && n.getId().equals(m.getId())))
                .count();
        long union = moviesA.size() + moviesB.size() - intersection;
        return union == 0 ? 0f : (float) intersection / union;
    }

    /**
     * Calcula qué componente de similitud es dominante para tipificar la arista del grafo.
     *
     * @param audio    score componente audio    (×0.35)
     * @param semantic score componente semántico (×0.25)
     * @param tag      score componente tags      (×0.20)
     * @param era      score componente era       (×0.10)
     * @param movie    score componente películas (×0.10)
     * @return tipo de arista dominante
     */
    public GraphEdge.EdgeType dominantEdgeType(float audio, float semantic, float tag, float era, float movie) {
        Map<GraphEdge.EdgeType, Float> weighted = Map.of(
                GraphEdge.EdgeType.AUDIO,    0.35f * audio,
                GraphEdge.EdgeType.SEMANTIC, 0.25f * semantic,
                GraphEdge.EdgeType.TAG,      0.20f * tag,
                GraphEdge.EdgeType.ERA,      0.10f * era,
                GraphEdge.EdgeType.MOVIE,    0.10f * movie
        );
        return weighted.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(GraphEdge.EdgeType.AUDIO);
    }

    // --- utilidades ---

    /**
     * Similitud coseno entre dos vectores de igual longitud.
     * Devuelve 0 si alguno de los vectores es el vector nulo.
     *
     * @param a primer vector
     * @param b segundo vector (debe tener la misma longitud que a)
     * @return similitud coseno en [0.0, 1.0]
     */
    private float cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        // Producto punto y normas en una sola pasada para eficiencia
        for (int i = 0; i < a.length; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        // Evitar división por cero cuando algún vector es nulo
        if (normA == 0 || normB == 0) return 0f;
        return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

}