package es.wara.ecos.backend.infrastructure.adapter.musicbrainz.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para una pista (track) dentro de un medio de MusicBrainz.
 * Cada track tiene asociada una grabación (recording) con su MBID propio.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
public class MusicBrainzTrackDTO {

    /** MBID de la pista (distinto del MBID de la grabación). */
    private String id;

    /** Posición en el disco, comenzando en 1. */
    private Integer position;

    /** Título de la pista tal como aparece en este release. */
    private String title;

    /** Duración en milisegundos. Puede ser null. */
    private Integer length;

    /**
     * Grabación vinculada a esta pista.
     * Usa recording.id para cruzar con Spotify: buscar "{title} {artist}" en Spotify.
     */
    private MusicBrainzRecordingDTO recording;

}
