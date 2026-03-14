package es.wara.ecos.backend.application.usecase.playlist;

import es.wara.ecos.backend.domain.model.Playlist;
import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.port.PlaylistRepository;
import es.wara.ecos.backend.domain.port.SpotifyAuthPort;
import es.wara.ecos.backend.domain.port.SpotifyPlaylistPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportPlaylistToSpotifyUseCaseTest {

    @Mock
    PlaylistRepository playlistRepository;

    @Mock
    SpotifyAuthPort spotifyAuthPort;

    @Mock
    SpotifyPlaylistPort spotifyPlaylistPort;

    @InjectMocks
    ExportPlaylistToSpotifyUseCase useCase;

    private static final UUID PLAYLIST_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID     = UUID.fromString("00000000-0000-0000-0000-000000000005");
    private static final UUID UNKNOWN_ID  = UUID.fromString("00000000-0000-0000-0000-000000000099");

    private Playlist buildPlaylist(List<Song> songs) {
        return new Playlist(PLAYLIST_ID, USER_ID, "Mi playlist", "descripción", null, LocalDateTime.now(), songs);
    }

    @Test
    void execute_fullFlow_assignsSpotifyExportId() {
        Song song = new Song();
        song.setSpotifyId("track-abc");

        Playlist playlist = buildPlaylist(List.of(song));
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        when(spotifyAuthPort.getTokenForUser(USER_ID)).thenReturn("user-token");
        when(spotifyPlaylistPort.getCurrentUserId("user-token")).thenReturn("spotify-user-id");
        when(spotifyPlaylistPort.createPlaylist("user-token", "spotify-user-id", "Mi playlist", "descripción"))
                .thenReturn("new-pl-id");
        when(playlistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Playlist result = useCase.execute(PLAYLIST_ID, USER_ID);

        assertThat(result.getSpotifyExportId()).isEqualTo("new-pl-id");
        verify(spotifyPlaylistPort).addTracksToPlaylist(
                eq("user-token"),
                eq("new-pl-id"),
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
    void execute_userWithoutSpotifyLinked_throwsIllegalArgumentException() {
        Playlist playlist = buildPlaylist(List.of());
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        when(spotifyAuthPort.getTokenForUser(USER_ID))
                .thenThrow(new IllegalArgumentException("El usuario no ha vinculado su cuenta de Spotify"));

        assertThatThrownBy(() -> useCase.execute(PLAYLIST_ID, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Spotify");
    }

    @Test
    void execute_songsWithoutSpotifyId_callsAddTracksWithEmptyList() {
        Song songNoId = new Song(); // spotifyId = null
        Playlist playlist = buildPlaylist(List.of(songNoId));
        when(playlistRepository.findById(PLAYLIST_ID)).thenReturn(Optional.of(playlist));
        when(spotifyAuthPort.getTokenForUser(USER_ID)).thenReturn("token");
        when(spotifyPlaylistPort.getCurrentUserId("token")).thenReturn("uid");
        when(spotifyPlaylistPort.createPlaylist(any(), any(), any(), any())).thenReturn("pid");
        when(playlistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(PLAYLIST_ID, USER_ID);

        verify(spotifyPlaylistPort).addTracksToPlaylist(eq("token"), eq("pid"), argThat(List::isEmpty));
    }

}
