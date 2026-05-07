package es.wara.ecos.backend.infrastructure.persistence.jpa;

import es.wara.ecos.backend.infrastructure.persistence.entity.PlaylistSongEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.PlaylistSongEntity.PlaylistSongId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio Spring Data JPA para PlaylistSongEntity; permite obtener las canciones de una playlist ordenadas por posición.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public interface JpaPlaylistSongRepository extends JpaRepository<PlaylistSongEntity, PlaylistSongId> {

    /** Carga las canciones de una playlist con sus datos completos (LEFT JOIN FETCH), ordenadas por posición. */
    @Query("SELECT ps FROM PlaylistSongEntity ps LEFT JOIN FETCH ps.song WHERE ps.id.playlistId = :playlistId ORDER BY ps.position ASC")
    List<PlaylistSongEntity> findById_PlaylistIdOrderByPosition(@Param("playlistId") UUID playlistId);

    /** Carga en bloque las canciones de múltiples playlists; evita N+1 al listar todas las playlists de un usuario. */
    @Query("SELECT ps FROM PlaylistSongEntity ps LEFT JOIN FETCH ps.song WHERE ps.id.playlistId IN :playlistIds ORDER BY ps.position ASC")
    List<PlaylistSongEntity> findByPlaylistIdIn(@Param("playlistIds") List<UUID> playlistIds);

    /**
     * Inserta una canción en la playlist asignándole automáticamente la siguiente posición
     * disponible (MAX(position) + 1) de forma atómica en una sola sentencia SQL.
     * Si la canción ya está en la playlist, la operación se ignora silenciosamente.
     *
     * @param playlistId identificador de la playlist destino
     * @param songId     identificador de la canción a añadir
     */
    @Modifying
    @Query(value = """
            INSERT INTO playlist_songs (playlist_id, song_id, position)
            SELECT :playlistId, :songId, COALESCE(MAX(position), 0) + 1
            FROM playlist_songs
            WHERE playlist_id = :playlistId
            ON CONFLICT (playlist_id, song_id) DO NOTHING
            """, nativeQuery = true)
    void addSongAtNextPosition(@Param("playlistId") UUID playlistId, @Param("songId") UUID songId);

}
