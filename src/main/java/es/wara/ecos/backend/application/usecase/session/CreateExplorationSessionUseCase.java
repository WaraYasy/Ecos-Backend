package es.wara.ecos.backend.application.usecase.session;

import es.wara.ecos.backend.domain.exception.ConflictException;
import es.wara.ecos.backend.domain.model.ExplorationSession;
import es.wara.ecos.backend.domain.model.SessionNode;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.port.SessionRepository;
import es.wara.ecos.backend.domain.port.SongRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    @Transactional
    public ExplorationSession execute(UUID userId, UUID startSongId, List<UUID> songIds, String name) {
        // Bloqueo pesimista: el conteo se hace dentro de la transacción con lock sobre las
        // filas del usuario, evitando que dos peticiones concurrentes burlen el límite de 2.
        if (sessionRepository.countByUserIdForUpdate(userId) >= MAX_SESSIONS) {
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

        // Una sola query para todas las canciones en lugar de N findById individuales
        List<UUID> ids = songIds != null ? songIds : List.of();
        Map<UUID, Song> songMap = songRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Song::getId, Function.identity()));

        List<SessionNode> nodes = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            Song song = songMap.get(ids.get(i));
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
