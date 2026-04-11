package es.wara.ecos.backend.application.usecase.session;

import es.wara.ecos.backend.domain.exception.ConflictException;
import es.wara.ecos.backend.domain.model.ExplorationSession;
import es.wara.ecos.backend.domain.model.SessionNode;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.port.SessionRepository;
import es.wara.ecos.backend.domain.port.SongRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso para guardar una sesión de exploración musical.
 * Aplica el límite de 2 sesiones por usuario.
 *
 * @author Wara Pacheco
 * @version 2.0
 * @since 1.0.0
 */
@Service
public class CreateExplorationSessionUseCase {

    static final int MAX_SESSIONS = 2;

    private final SessionRepository sessionRepository;
    private final SongRepository songRepository;

    public CreateExplorationSessionUseCase(SessionRepository sessionRepository, SongRepository songRepository) {
        this.sessionRepository = sessionRepository;
        this.songRepository = songRepository;
    }

    /**
     * Guarda una sesión de exploración con la lista de canciones visitadas.
     *
     * @param userId      usuario que guarda la sesión
     * @param startSongId canción inicial del grafo
     * @param songIds     lista ordenada de canciones visitadas
     * @param name        nombre opcional dado por el usuario
     * @return la sesión persistida con canciones completas
     * @throws ConflictException        si el usuario ya tiene 2 sesiones guardadas
     * @throws IllegalArgumentException si la canción inicial no existe
     */
    public ExplorationSession execute(UUID userId, UUID startSongId, List<UUID> songIds, String name) {
        if (sessionRepository.countByUserId(userId) >= MAX_SESSIONS) {
            throw new ConflictException(
                    "Ya tienes " + MAX_SESSIONS + " sesiones guardadas. Elimina una para poder guardar otra.");
        }

        songRepository.findById(startSongId)
                .orElseThrow(() -> new IllegalArgumentException("Canción inicial no encontrada: " + startSongId));

        ExplorationSession session = new ExplorationSession();
        session.setUserId(userId);
        session.setStartSongId(startSongId);
        session.setName(name);
        session.setCreatedAt(LocalDateTime.now());

        List<SessionNode> nodes = new ArrayList<>();
        List<UUID> ids = songIds != null ? songIds : List.of();
        for (int i = 0; i < ids.size(); i++) {
            Song song = songRepository.findById(ids.get(i)).orElse(null);
            if (song != null) {
                SessionNode node = new SessionNode();
                node.setSong(song);
                node.setSequenceOrder(i + 1);
                nodes.add(node);
            }
        }
        session.setNodes(nodes);

        return sessionRepository.save(session);
    }

}
