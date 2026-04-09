package es.wara.ecos.backend.domain.port;

import java.util.UUID;

/**
 * Puerto de salida para solicitar la generación de embeddings semánticos al microservicio de IA.
 *
 * <p>La implementación ({@code EmbeddingClient}) llama al micro Python vía HTTP.
 * Si el micro no está disponible, la operación se ignora sin lanzar excepción
 * — el guardado de la canción no debe bloquearse por indisponibilidad del micro.</p>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
public interface EmbeddingProvider {

    /**
     * Solicita al micro de IA que genere y persista el embedding de una canción.
     * La llamada es best-effort: si falla, se registra en log pero no propaga.
     *
     * @param songId UUID de la canción recién persistida
     * @param text   texto a embedar: {@code "título · artista1, artista2 · tag1 tag2 · género1 género2"}
     */
    void requestEmbedding(UUID songId, String text);
}