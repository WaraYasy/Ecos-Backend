package es.wara.ecos.backend.application.usecase.user;

import es.wara.ecos.backend.domain.model.ExplorationSession;
import es.wara.ecos.backend.domain.model.MusicProfile;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.port.SessionRepository;
import es.wara.ecos.backend.domain.port.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Caso de uso para calcular el perfil musical de un usuario a partir de su historial
 * de sesiones de exploración.
 * <p>
 * Calcula: energía media, riqueza armónica (diversidad de tonalidades), eclecticidad
 * (diversidad de géneros), décadas favoritas y top 5 géneros.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Service
public class CalculateUserProfileUseCase {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;

    public CalculateUserProfileUseCase(UserRepository userRepository, SessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Calcula el perfil musical del usuario a partir de las canciones visitadas
     * en sus sesiones de exploración.
     *
     * @param userId identificador del usuario
     * @return el perfil musical calculado
     * @throws IllegalArgumentException si el usuario no existe
     */
    public MusicProfile execute(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        List<Song> songs = collectSongsFromSessions(sessionRepository.findByUserId(userId));

        return new MusicProfile(
                userId,
                calculateAvgEnergy(songs),
                calculateHarmonicRichness(songs),
                calculateEclecticity(songs),
                calculateFavoriteEras(songs),
                calculateTopGenres(songs)
        );
    }

    /** Extrae todas las canciones únicas de las sesiones del usuario. */
    private List<Song> collectSongsFromSessions(List<ExplorationSession> sessions) {
        return sessions.stream()
                .filter(s -> s.getNodes() != null)
                .flatMap(s -> s.getNodes().stream())
                .map(node -> node.getSong())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /** Energía media de las canciones con audio features disponibles (0.0 – 1.0). */
    private BigDecimal calculateAvgEnergy(List<Song> songs) {
        OptionalDouble avg = songs.stream()
                .filter(s -> s.getAudioFeatures() != null && s.getAudioFeatures().getEnergy() != null)
                .mapToDouble(s -> s.getAudioFeatures().getEnergy().doubleValue())
                .average();
        return avg.isPresent()
                ? BigDecimal.valueOf(avg.getAsDouble()).setScale(4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    /**
     * Riqueza armónica: proporción de tonalidades únicas sobre las 12 posibles (0.0 – 1.0).
     * Un valor alto indica variedad de tonalidades en el historial.
     */
    private BigDecimal calculateHarmonicRichness(List<Song> songs) {
        long uniqueKeys = songs.stream()
                .filter(s -> s.getAudioFeatures() != null && s.getAudioFeatures().getMusicalKey() != null)
                .map(s -> s.getAudioFeatures().getMusicalKey())
                .distinct()
                .count();
        return BigDecimal.valueOf(uniqueKeys / 12.0).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Eclecticidad: proporción de géneros únicos sobre el total de etiquetas (0.0 – 1.0).
     * Un valor alto indica una escucha muy variada en géneros.
     */
    private BigDecimal calculateEclecticity(List<Song> songs) {
        long totalTags = songs.stream()
                .filter(s -> s.getTags() != null)
                .mapToLong(s -> s.getTags().size())
                .sum();
        if (totalTags == 0) {
            return BigDecimal.ZERO;
        }
        long uniqueGenres = songs.stream()
                .filter(s -> s.getTags() != null)
                .flatMap(s -> s.getTags().stream())
                .map(t -> t.getName())
                .distinct()
                .count();
        double ratio = Math.min(1.0, (double) uniqueGenres / totalTags);
        return BigDecimal.valueOf(ratio).setScale(4, RoundingMode.HALF_UP);
    }

    /** Distribución normalizada de escuchas por década, ej. {"1980s": 0.4, "1990s": 0.3}. */
    private Map<String, Double> calculateFavoriteEras(List<Song> songs) {
        List<Song> withYear = songs.stream()
                .filter(s -> s.getAlbum() != null && s.getAlbum().getYear() != null)
                .collect(Collectors.toList());
        if (withYear.isEmpty()) {
            return Collections.emptyMap();
        }
        double total = withYear.size();
        return withYear.stream()
                .collect(Collectors.groupingBy(
                        s -> (s.getAlbum().getYear() / 10 * 10) + "s",
                        Collectors.counting()
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Math.round(e.getValue() / total * 100.0) / 100.0
                ));
    }

    /** Los 5 géneros/etiquetas más frecuentes en el historial del usuario. */
    private List<String> calculateTopGenres(List<Song> songs) {
        return songs.stream()
                .filter(s -> s.getTags() != null)
                .flatMap(s -> s.getTags().stream())
                .collect(Collectors.groupingBy(t -> t.getName(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

}
