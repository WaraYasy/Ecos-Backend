package es.wara.ecos.backend.domain.port;

import es.wara.ecos.backend.domain.model.SongEmbedding;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida para persistencia y consulta de embeddings semánticos de canciones.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public interface SongEmbeddingRepository {

    /**
     * Recupera el embedding de una canción por su ID.
     *
     * @param songId identificador de la canción
     * @return el embedding si existe
     */
    Optional<SongEmbedding> findBySongId(UUID songId);

    /**
     * Persiste o actualiza el embedding de una canción.
     *
     * @param embedding embedding a guardar
     */
    void save(SongEmbedding embedding);

    /**
     * Recupera los embeddings de múltiples canciones en una sola query IN.
     * Usar en lugar de N llamadas a {@link #findBySongId} para evitar el problema N+1.
     *
     * @param songIds lista de IDs de canciones a buscar
     * @return lista de embeddings encontrados (puede ser menor que songIds si alguno no existe)
     */
    List<SongEmbedding> findAllBySongIds(List<UUID> songIds);

    /**
     * Busca las canciones cuyos embeddings son más cercanos al vector dado (similitud coseno).
     *
     * @param queryVector vector de consulta de 384 dimensiones
     * @param limit       número máximo de resultados
     * @param excludeIds  IDs a excluir de los resultados (ej. canción raíz)
     * @return lista de IDs de canciones ordenados por similitud descendente
     */
    List<UUID> findNearestNeighbors(float[] queryVector, int limit, List<UUID> excludeIds);

}