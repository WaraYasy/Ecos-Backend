package es.wara.ecos.backend.interfaces.rest.dto;

import es.wara.ecos.backend.domain.model.Playlist;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta para una playlist de ECOS.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public record PlaylistResponse(
        Long id,
        String name,
        String description,
        Long userId,
        LocalDateTime createdAt,
        List<SongResponse> songs
) {

    public static PlaylistResponse from(Playlist playlist) {
        return new PlaylistResponse(
                playlist.getId(),
                playlist.getName(),
                playlist.getDescription(),
                playlist.getUserId(),
                playlist.getCreatedAt(),
                playlist.getSongs() != null
                        ? playlist.getSongs().stream().map(SongResponse::from).toList()
                        : List.of()
        );
    }

}
