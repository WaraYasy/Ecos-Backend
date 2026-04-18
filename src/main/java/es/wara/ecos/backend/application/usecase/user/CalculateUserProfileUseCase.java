package es.wara.ecos.backend.application.usecase.user;

import es.wara.ecos.backend.domain.model.ExplorationSession;
import es.wara.ecos.backend.domain.model.MusicProfile;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.port.SessionRepository;
import es.wara.ecos.backend.domain.port.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.wara.ecos.backend.infrastructure.adapter.ai.ProfileClient;
import es.wara.ecos.backend.infrastructure.persistence.entity.UserEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.UserProfileEntity;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaUserProfileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.UUID;
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
@Slf4j
@Service
public class CalculateUserProfileUseCase {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final ProfileClient profileClient;
    private final JpaUserProfileRepository userProfileRepository;
    private final ObjectMapper objectMapper;

    public CalculateUserProfileUseCase(UserRepository userRepository,
                                       SessionRepository sessionRepository,
                                       ProfileClient profileClient,
                                       JpaUserProfileRepository userProfileRepository,
                                       ObjectMapper objectMapper) {
        this.userRepository        = userRepository;
        this.sessionRepository     = sessionRepository;
        this.profileClient         = profileClient;
        this.userProfileRepository = userProfileRepository;
        this.objectMapper          = objectMapper;
    }

    /**
     * Calcula el perfil musical del usuario a partir de las canciones visitadas
     * en sus sesiones de exploración.
     *
     * @param userId identificador del usuario
     * @return el perfil musical calculado
     * @throws IllegalArgumentException si el usuario no existe
     */
    public MusicProfile execute(UUID userId) {
        log.debug("CalculateUserProfile: inicio userId={}", userId);
        StopWatch sw = new StopWatch("CalculateUserProfile");

        sw.start("findUser");
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("Usuario no encontrado: " + userId);
        }
        sw.stop();

        // Intentar micro primero
        sw.start("microProfile");
        var microProfile = profileClient.getProfile(userId);
        sw.stop();

        if (microProfile.isPresent()) {
            log.debug("CalculateUserProfile: perfil obtenido del micro para userId={} → {}ms",
                    userId, sw.getLastTaskTimeMillis());
            persist(userId, microProfile.get());
            return microProfile.get();
        }

        // Fallback: cálculo local desde sesiones
        log.debug("CalculateUserProfile: micro no disponible, calculando localmente userId={}", userId);

        sw.start("fetchSessions");
        List<ExplorationSession> sessions = sessionRepository.findByUserId(userId);
        sw.stop();
        log.debug("CalculateUserProfile: {} sesiones cargadas para userId={} → {}ms",
                sessions.size(), userId, sw.getLastTaskTimeMillis());

        sw.start("collectSongs");
        List<Song> songs = collectSongsFromSessions(sessions);
        sw.stop();
        log.debug("CalculateUserProfile: {} canciones únicas extraídas → {}ms",
                songs.size(), sw.getLastTaskTimeMillis());

        sw.start("calculations");
        MusicProfile profile = new MusicProfile(
                userId,
                calculateAvgEnergy(songs),
                calculateHarmonicRichness(songs),
                calculateEclecticity(songs),
                calculateFavoriteEras(songs),
                calculateTopGenres(songs)
        );
        sw.stop();

        long total = sw.getTotalTimeMillis();
        if (total > 500) {
            log.warn("CalculateUserProfile: LENTO userId={} | total={}ms | desglose: {}",
                    userId, total, sw.prettyPrint());
        } else {
            log.debug("CalculateUserProfile: completado userId={} → {}ms total", userId, total);
        }

        persist(userId, profile);
        return profile;
    }

    /**
     * Versión asíncrona de {@link #execute(UUID)}: lanza el cálculo en el pool
     * {@code exportExecutor} sin bloquear al llamador.
     * <p>
     * Diseñado para dispararse desde {@code POST /v1/sessions} tras guardar una sesión,
     * de forma que el perfil siempre refleje los últimos datos sin penalizar la respuesta HTTP.
     * Los errores se absorben con un log de advertencia para no propagar excepciones al pool.
     * </p>
     *
     * @param userId identificador del usuario cuyo perfil se recalcula
     */
    @Async("exportExecutor")
    public void executeAsync(UUID userId) {
        try {
            execute(userId);
        } catch (Exception e) {
            log.warn("CalculateUserProfile async falló para userId={}: {}", userId, e.getMessage());
        }
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

    /** Serializa un objeto a JSON para almacenarlo en columnas JSONB; devuelve {@code null} si falla. */
    private String toJson(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { return null; }
    }

    /**
     * Persiste el perfil calculado en {@code user_profiles}.
     * Si la fila ya existe la actualiza (upsert manual); si no, la crea asociando el {@code UserEntity}.
     * Los errores no se propagan para no interrumpir el flujo principal ni el async.
     */
    private void persist(UUID userId, MusicProfile profile) {
        try {
            UserProfileEntity entity = userProfileRepository.findById(userId)
                    .orElseGet(() -> {
                        UserProfileEntity e = new UserProfileEntity();
                        UserEntity user = new UserEntity();
                        user.setId(userId);
                        e.setUser(user);
                        return e;
                    });
            entity.setAvgEnergy(profile.getAvgEnergy());
            entity.setHarmonicRichness(profile.getHarmonicRichness());
            entity.setEclecticity(profile.getEclecticity());
            if (profile.getFavoriteEras() != null) {
                entity.setFavoriteEras(toJson(profile.getFavoriteEras()));
            }
            if (profile.getTopGenres() != null) {
                entity.setTopGenres(toJson(profile.getTopGenres()));
            }
            userProfileRepository.save(entity);
        } catch (Exception e) {
            log.warn("No se pudo persistir user_profile para userId={}: {}", userId, e.getMessage());
        }
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
