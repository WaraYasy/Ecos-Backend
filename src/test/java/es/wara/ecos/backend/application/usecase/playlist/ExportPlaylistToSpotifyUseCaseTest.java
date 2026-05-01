package es.wara.ecos.backend.application.usecase.playlist;

import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.model.User;
import es.wara.ecos.backend.domain.port.PlaylistRepository;
import es.wara.ecos.backend.domain.port.SpotifyAuthPort;
import es.wara.ecos.backend.domain.port.SpotifyPlaylistPort;
import es.wara.ecos.backend.domain.port.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportPlaylistToSpotifyUseCaseTest {

    @Mock PlaylistRepository  playlistRepository;
    @Mock UserRepository      userRepository;
    @Mock SpotifyAuthPort     spotifyAuthPort;
    @Mock SpotifyPlaylistPort spotifyPlaylistPort;

    @InjectMocks
    ExportPlaylistToSpotifyUseCase useCase;

    private static final UUID PLAYLIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID     = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID UNKNOWN_ID  = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private Playlist buildPlaylist(String description, List<Song> songs) {
        return new Playlist(PLAYLIST_ID, USER_ID, "Mi playlist", description, null, LocalDateTime.now(), songs);
    }

    private User buildUser(String language) {
        User user = new User();
        user.setId(USER_ID);
        user.setLanguage(language);
        return user;
    }

    private void stubBaseFlow(String language, String expectedDescription) {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(buildUser(language)));
        when(spotifyAuthPort.getTokenForUser(USER_ID)).thenReturn("token");
        when(spotifyPlaylistPort.getCurrentUserId("token")).thenReturn("uid");
        when(spotifyPlaylistPort.createPlaylist(any(), any(), any(), eq(expectedDescription))).thenReturn("pid");
        when(playlistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void execute_fullFlow_assignsSpotifyExportId() {
        Song song = new Song();
        song.setSpotifyId("track-abc");

        Playlist playlist = buildPlaylist("descripción propia", List.of(song));
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        stubBaseFlow("es", "descripción propia");

        Playlist result = useCase.execute(PLAYLIST_ID, USER_ID);

        assertThat(result.getSpotifyExportId()).isEqualTo("pid");
        verify(spotifyPlaylistPort).addTracksToPlaylist(
                eq("token"), eq("pid"),
                argThat(uris -> uris.size() == 1 && uris.get(0).equals("spotify:track:track-abc"))
        );
    }

    @Test
    void execute_playlistNotFound_throwsIllegalArgumentException() {
        when(playlistRepository.findById(UNKNOWN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(UNKNOWN_ID, USER_ID))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(spotifyAuthPort, spotifyPlaylistPort);
    }

    @Test
    void execute_userNotFound_throwsIllegalArgumentException() {
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(buildPlaylist(null, List.of())));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(PLAYLIST_ID, USER_ID))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(spotifyAuthPort, spotifyPlaylistPort);
    }

    @Test
    void execute_userWithoutSpotifyLinked_throwsIllegalArgumentException() {
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(buildPlaylist(null, List.of())));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(buildUser("es")));
        when(spotifyAuthPort.getTokenForUser(USER_ID))
                .thenThrow(new IllegalArgumentException("El usuario no ha vinculado su cuenta de Spotify"));

        assertThatThrownBy(() -> useCase.execute(PLAYLIST_ID, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Spotify");
    }

    @Test
    void execute_songsWithoutSpotifyId_callsAddTracksWithEmptyList() {
        Song songNoId = new Song(); // spotifyId = null
        Playlist playlist = buildPlaylist(null, List.of(songNoId));
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        stubBaseFlow(null, "Desde el universo de ECOS");

        useCase.execute(PLAYLIST_ID, USER_ID);

        verify(spotifyPlaylistPort).addTracksToPlaylist(eq("token"), eq("pid"), argThat(List::isEmpty));
    }

    // ── descripción: playlist propia vs. fallback por idioma ──────────────────

    @Test
    void execute_playlistWithDescription_usesPlaylistDescription() {
        Playlist playlist = buildPlaylist("Mi descripción personalizada", List.of());
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        stubBaseFlow("es", "Mi descripción personalizada");

        useCase.execute(PLAYLIST_ID, USER_ID);

        verify(spotifyPlaylistPort).createPlaylist(any(), any(), any(), eq("Mi descripción personalizada"));
    }

    @Test
    void execute_playlistWithoutDescription_spanishUser_usesSpanishFallback() {
        Playlist playlist = buildPlaylist(null, List.of());
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        stubBaseFlow("es", "Desde el universo de ECOS");

        useCase.execute(PLAYLIST_ID, USER_ID);

        verify(spotifyPlaylistPort).createPlaylist(any(), any(), any(), eq("Desde el universo de ECOS"));
    }

    @Test
    void execute_playlistWithoutDescription_englishUser_usesEnglishFallback() {
        Playlist playlist = buildPlaylist(null, List.of());
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        stubBaseFlow("en", "From the ECOS universe");

        useCase.execute(PLAYLIST_ID, USER_ID);

        verify(spotifyPlaylistPort).createPlaylist(any(), any(), any(), eq("From the ECOS universe"));
    }

    // ── portada ───────────────────────────────────────────────────────────────

    @Test
    void execute_withCoverImageLoaded_uploadsCover() {
        byte[] fakeJpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, 0x01};
        ReflectionTestUtils.setField(useCase, "coverImageBytes", fakeJpeg);

        Playlist playlist = buildPlaylist(null, List.of());
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        stubBaseFlow("es", "Desde el universo de ECOS");

        useCase.execute(PLAYLIST_ID, USER_ID);

        verify(spotifyPlaylistPort).uploadPlaylistCover(eq("token"), eq("pid"), eq(fakeJpeg));
    }

    @Test
    void execute_withNoCoverImage_skipsUpload() {
        // coverImageBytes permanece null (PostConstruct no se ejecuta en tests unitarios)
        Playlist playlist = buildPlaylist(null, List.of());
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        stubBaseFlow("es", "Desde el universo de ECOS");

        useCase.execute(PLAYLIST_ID, USER_ID);

        verify(spotifyPlaylistPort, never()).uploadPlaylistCover(any(), any(), any());
    }

    @Test
    void execute_playlistWithBlankDescription_nullLanguage_usesSpanishFallback() {
        Playlist playlist = buildPlaylist("   ", List.of());
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        stubBaseFlow(null, "Desde el universo de ECOS");

        useCase.execute(PLAYLIST_ID, USER_ID);

        verify(spotifyPlaylistPort).createPlaylist(any(), any(), any(), eq("Desde el universo de ECOS"));
    }

}