package es.wara.ecos.backend.application.usecase.session;

import es.wara.ecos.backend.domain.model.ExplorationSession;
import es.wara.ecos.backend.domain.port.SessionRepository;
import es.wara.ecos.backend.domain.port.SongRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Caso de uso para iniciar una nueva sesión de exploración a partir de una canción semilla.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class CreateExplorationSessionUseCase {

    private final SessionRepository sessionRepository;
    private final SongRepository songRepository;

    public CreateExplorationSessionUseCase(SessionRepository sessionRepository, SongRepository songRepository) {
        this.sessionRepository = sessionRepository;
        this.songRepository = songRepository;
    }

    /**
     * Crea y persiste una nueva sesión de exploración.
     *
     * @param userId      identificador del usuario que inicia la sesión
     * @param startSongId identificador de la canción de inicio del grafo
     * @return la sesión creada con su identificador asignado
     * @throws IllegalArgumentException si la canción inicial no existe
     */
    public ExplorationSession execute(UUID userId, UUID startSongId) {
        songRepository.findById(startSongId)
                .orElseThrow(() -> new IllegalArgumentException("Canción inicial no encontrada: " + startSongId));

        ExplorationSession session = new ExplorationSession();
        session.setUserId(userId);
        session.setStartSongId(startSongId);
        session.setCreatedAt(LocalDateTime.now());
        session.setNodes(new ArrayList<>());
        return sessionRepository.save(session);
    }

}
