package es.wara.ecos.backend.interfaces.rest.controller;

import es.wara.ecos.backend.application.usecase.search.GetSimilarSongsUseCase;
import es.wara.ecos.backend.application.usecase.song.GetSongByIdUseCase;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.interfaces.rest.dto.AudioFeaturesResponse;
import es.wara.ecos.backend.interfaces.rest.dto.SongResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para operaciones sobre canciones individuales.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@RestController
@RequestMapping("/v1/songs")
public class SongController {

    private final GetSongByIdUseCase getSongByIdUseCase;
    private final GetSimilarSongsUseCase getSimilarSongsUseCase;

    public SongController(GetSongByIdUseCase getSongByIdUseCase,
                          GetSimilarSongsUseCase getSimilarSongsUseCase) {
        this.getSongByIdUseCase = getSongByIdUseCase;
        this.getSimilarSongsUseCase = getSimilarSongsUseCase;
    }

    @GetMapping("/{id}")
    public ResponseEntity<SongResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(SongResponse.from(getSongByIdUseCase.execute(id)));
    }

    @GetMapping("/{id}/related")
    public ResponseEntity<List<SongResponse>> getRelated(@PathVariable Long id) {
        List<SongResponse> related = getSimilarSongsUseCase.execute(id)
                .stream()
                .map(SongResponse::from)
                .toList();
        return ResponseEntity.ok(related);
    }

    @GetMapping("/{id}/audio-features")
    public ResponseEntity<AudioFeaturesResponse> getAudioFeatures(@PathVariable Long id) {
        Song song = getSongByIdUseCase.execute(id);
        AudioFeaturesResponse response = AudioFeaturesResponse.from(song.getAudioFeatures());
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

}
