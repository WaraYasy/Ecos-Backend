package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.SongEmbeddingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para SongEmbeddingEntity.
 * La búsqueda de vecinos más cercanos usa una native query con el operador <=> de pgvector
 * (distancia coseno), que requiere el índice ivfflat de V25.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public interface JpaSongEmbeddingRepository extends JpaRepository<SongEmbeddingEntity, UUID> {

    /**
     * Retorna los IDs de canciones con embedding más cercano al vector dado,
     * excluyendo los IDs especificados, ordenados por similitud coseno descendente.
     *
     * @param queryVector vector de consulta en formato "[v1,v2,...,v384]"
     * @param excludeIds  IDs a excluir (canción raíz y ya presentes en el grafo)
     * @param limit       número máximo de resultados
     * @return lista de song_id ordenados por distancia coseno ascendente
     */
    @Query(value = """
            SELECT song_id
            FROM song_embeddings
            WHERE song_id NOT IN (:excludeIds)
            ORDER BY embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<UUID> findNearestNeighbors(
            @Param("queryVector") String queryVector,
            @Param("excludeIds") List<UUID> excludeIds,
            @Param("limit") int limit
    );

    @Query(value = """
            SELECT song_id
            FROM song_embeddings
            ORDER BY embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<UUID> findNearestNeighborsNoExclusions(
            @Param("queryVector") String queryVector,
            @Param("limit") int limit
    );

}