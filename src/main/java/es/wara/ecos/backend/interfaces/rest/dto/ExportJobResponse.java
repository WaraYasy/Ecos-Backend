package es.wara.ecos.backend.interfaces.rest.dto;

import es.wara.ecos.backend.application.service.ExportJob;

/**
 * DTO de respuesta para el estado de un job de exportación a Spotify.
 * Devuelto por {@code POST /v1/playlists/{id}/export/spotify} (202) y
 * {@code GET /v1/playlists/{id}/export/spotify/{jobId}} (200).
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
public record ExportJobResponse(
        String jobId,
        String status,
        String spotifyPlaylistId,
        String error
) {
    public static ExportJobResponse from(ExportJob job) {
        return new ExportJobResponse(
                job.getJobId(),
                job.getStatus().name(),
                job.getSpotifyPlaylistId(),
                job.getError()
        );
    }
}