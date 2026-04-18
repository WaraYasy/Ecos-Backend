package es.wara.ecos.backend.application.usecase.user;

import es.wara.ecos.backend.domain.model.Album;
import es.wara.ecos.backend.domain.model.AudioFeatures;
import es.wara.ecos.backend.domain.model.ExplorationSession;
import es.wara.ecos.backend.domain.model.MusicProfile;
import es.wara.ecos.backend.domain.model.SessionNode;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.model.Tag;
import com.fasterxml.jackson.databind.ObjectMapper;
import es.wara.ecos.backend.domain.port.SessionRepository;
import es.wara.ecos.backend.domain.port.UserRepository;
import es.wara.ecos.backend.infrastructure.adapter.ai.ProfileClient;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaUserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculateUserProfileUseCaseTest {

    @Mock UserRepository userRepository;
    @Mock SessionRepository sessionRepository;
    @Mock ProfileClient profileClient;
    @Mock JpaUserProfileRepository userProfileRepository;

    CalculateUserProfileUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CalculateUserProfileUseCase(
                userRepository, sessionRepository,
                profileClient, userProfileRepository,
                new ObjectMapper());
        // Por defecto el micro no está disponible → ejercita el cálculo local
        // lenient: el test de usuario no encontrado lanza antes de llegar a estos stubs
        lenient().when(profileClient.getProfile(any())).thenReturn(Optional.empty());
        lenient().when(userProfileRepository.findById(any())).thenReturn(Optional.empty());
        lenient().when(userProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static final UUID USER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID UNKNOWN_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    // ─── helpers ──────────────────────────────────────────────────────────────

    private Song songWith(BigDecimal energy, Integer musicalKey, List<Tag> tags, Integer albumYear) {
        Song s = new Song();
        AudioFeatures af = new AudioFeatures();
        af.setEnergy(energy);
        af.setMusicalKey(musicalKey);
        s.setAudioFeatures(af);
        s.setTags(tags);
        if (albumYear != null) {
            Album album = new Album();
            album.setYear(albumYear);
            s.setAlbum(album);
        }
        return s;
    }

    private ExplorationSession sessionWith(List<Song> songs) {
        List<SessionNode> nodes = songs.stream().map(song -> {
            SessionNode node = new SessionNode();
            node.setSong(song);
            return node;
        }).collect(Collectors.toList());
        return new ExplorationSession(SESSION_ID, USER_ID, UUID.randomUUID(), null, LocalDateTime.now(), nodes);
    }

    // ─── tests ────────────────────────────────────────────────────────────────

    @Test
    void execute_userNotFound_throwsIllegalArgumentException() {
        when(userRepository.existsById(UNKNOWN_ID)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(UNKNOWN_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void execute_noSessions_returnsZeroProfile() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(sessionRepository.findByUserId(USER_ID)).thenReturn(List.of());

        MusicProfile result = useCase.execute(USER_ID);

        assertThat(result.getAvgEnergy()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getHarmonicRichness()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getEclecticity()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getFavoriteEras()).isEmpty();
        assertThat(result.getTopGenres()).isEmpty();
    }

    @Test
    void execute_songsWithAudioFeatures_calculatesAvgEnergyCorrectly() {
        Song s1 = songWith(new BigDecimal("0.4"), 0, List.of(), null);
        Song s2 = songWith(new BigDecimal("0.6"), 2, List.of(), null);
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(sessionRepository.findByUserId(USER_ID)).thenReturn(List.of(sessionWith(List.of(s1, s2))));

        MusicProfile result = useCase.execute(USER_ID);

        assertThat(result.getAvgEnergy()).isEqualByComparingTo(new BigDecimal("0.5000"));
    }

    @Test
    void execute_songsWithTags_calculatesTopGenresAndEclecticity() {
        Tag rock = new Tag("rock", BigDecimal.ONE);
        Tag pop = new Tag("pop", new BigDecimal("0.8"));
        // s1 tiene 3 tags "rock", s2 tiene 1 tag "pop" → total 4, géneros únicos 2
        Song s1 = songWith(null, null, List.of(rock, rock, rock), null);
        Song s2 = songWith(null, null, List.of(pop), null);
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(sessionRepository.findByUserId(USER_ID)).thenReturn(List.of(sessionWith(List.of(s1, s2))));

        MusicProfile result = useCase.execute(USER_ID);

        assertThat(result.getTopGenres()).isNotEmpty();
        assertThat(result.getTopGenres().get(0)).isEqualTo("rock");
        // min(1.0, 2/4) = 0.5
        assertThat(result.getEclecticity()).isEqualByComparingTo(new BigDecimal("0.5000"));
    }

    @Test
    void execute_songsWithAlbumYears_calculatesFavoriteEras() {
        Song s1 = songWith(null, null, List.of(), 1985);
        Song s2 = songWith(null, null, List.of(), 1987);
        Song s3 = songWith(null, null, List.of(), 1995);
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(sessionRepository.findByUserId(USER_ID)).thenReturn(List.of(sessionWith(List.of(s1, s2, s3))));

        MusicProfile result = useCase.execute(USER_ID);

        assertThat(result.getFavoriteEras()).containsKey("1980s");
        assertThat(result.getFavoriteEras()).containsKey("1990s");
        assertThat(result.getFavoriteEras().get("1980s")).isCloseTo(0.67, within(0.01));
        assertThat(result.getFavoriteEras().get("1990s")).isCloseTo(0.33, within(0.01));
    }

}
