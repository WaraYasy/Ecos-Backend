package es.wara.ecos.backend.application.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa un job de exportación de playlist a Spotify en la cola asíncrona.
 * <p>
 * Se persiste en Redis con TTL de 1 hora. El front hace polling sobre
 * {@code GET /v1/playlists/{id}/export/spotify/{jobId}} hasta que el estado
 * sea {@code DONE} o {@code FAILED}.
 * </p>
 *
 * @author Wara Pacheco
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExportJob {

    /** Ciclo de vida del job: creado → procesando → completado o fallido. */
    public enum Status { PENDING, PROCESSING, DONE, FAILED }

    private String jobId;
    private UUID playlistId;
    private UUID userId;
    private Status status;
    /** ID de la playlist creada en Spotify; disponible solo cuando {@code status == DONE}. */
    private String spotifyPlaylistId;
    /** Mensaje de error; disponible solo cuando {@code status == FAILED}. */
    private String error;
    private LocalDateTime createdAt;
}