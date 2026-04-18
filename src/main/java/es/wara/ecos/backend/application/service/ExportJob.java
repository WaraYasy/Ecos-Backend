package es.wara.ecos.backend.application.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExportJob {

    public enum Status { PENDING, PROCESSING, DONE, FAILED }

    private String jobId;
    private UUID playlistId;
    private UUID userId;
    private Status status;
    private String spotifyPlaylistId;
    private String error;
    private LocalDateTime createdAt;
}