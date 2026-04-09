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
     * Busca canciones semánticamente similares al texto dado.
     *
     * @param query texto de búsqueda en cualquier idioma
     * @param limit número máximo de resultados
     * @return lista de UUIDs ordenados por relevancia, o vacía si el micro no responde
     */
    List<UUID> search(String query, int limit);
}