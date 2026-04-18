package es.wara.ecos.backend.interfaces.rest.dto;

import es.wara.ecos.backend.application.service.ExportJob;

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