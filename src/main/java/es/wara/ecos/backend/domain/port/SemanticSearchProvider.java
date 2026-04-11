package es.wara.ecos.backend.domain.port;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de salida para búsqueda semántica en el microservicio de IA.
 *
 * <p>Devuelve UUIDs de canciones ordenados por similitud coseno con el texto de consulta.
 * Si el micro no está disponible, devuelve lista vacía — la búsqueda SQL continúa sin alterarse.</p>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
public interface SemanticSearchProvider {

    /**
     * Búsqueda semántica directa: codifica la query y busca vectores más cercanos.
     *
     * @param query texto de búsqueda en cualquier idioma
     * @param limit número máximo de resultados (1–50)
     * @return lista de UUIDs ordenados por relevancia, o vacía si el micro no responde
     */
    List<UUID> search(String query, int limit);

    /**
     * Búsqueda semántica con expansión de query vía LLM (modo imaginario).
     * Convierte la consulta en keywords semánticos antes de buscar — ideal para
     * queries evocadoras ("canciones tristes", "música para una boda").
     * Tarda ~400–700ms más que {@link #search} por la llamada al LLM.
     *
     * @param query texto evocador en cualquier idioma
     * @param limit número máximo de resultados (1–50)
     * @return lista de UUIDs ordenados por relevancia, o vacía si el micro no responde o el LLM no está disponible
     */
    List<UUID> searchEnhanced(String query, int limit);
}