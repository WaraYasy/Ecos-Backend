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

    private Playlist buildPlaylist(List<Song> songs) {
        return new Playlist(1L, 5L, "Mi playlist", "descripción", null, LocalDateTime.now(), songs);
    }

    @Test
    void execute_fullFlow_assignsSpotifyExportId() {
        Song song = new Song();
        song.setSpotifyId("track-abc");

        Playlist playlist = buildPlaylist(List.of(song));
        when(playlistRepository.findById(1L)).thenReturn(Optional.of(playlist));
        when(spotifyAuthPort.getTokenForUser(5L)).thenReturn("user-token");
        when(spotifyPlaylistPort.getCurrentUserId("user-token")).thenReturn("spotify-user-id");
        when(spotifyPlaylistPort.createPlaylist("user-token", "spotify-user-id", "Mi playlist", "descripción"))
                .thenReturn("new-pl-id");
        when(playlistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Playlist result = useCase.execute(1L, 5L);

        assertThat(result.getSpotifyExportId()).isEqualTo("new-pl-id");
        verify(spotifyPlaylistPort).addTracksToPlaylist(
                eq("user-token"),
                eq("new-pl-id"),
                argThat(uris -> uris.size() == 1 && uris.get(0).equals("spotify:track:track-abc"))
        );
    }

    @Test
    void execute_playlistNotFound_throwsIllegalArgumentException() {
        when(playlistRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(99L, 5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");

        verifyNoInteractions(spotifyAuthPort, spotifyPlaylistPort);
    }

    @Test
    void execute_userWithoutSpotifyLinked_throwsIllegalArgumentException() {
        Playlist playlist = buildPlaylist(List.of());
        when(playlistRepository.findById(1L)).thenReturn(Optional.of(playlist));
        when(spotifyAuthPort.getTokenForUser(5L))
                .thenThrow(new IllegalArgumentException("El usuario 5 no ha vinculado su cuenta de Spotify"));

        assertThatThrownBy(() -> useCase.execute(1L, 5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Spotify");
    }

    @Test
    void execute_songsWithoutSpotifyId_callsAddTracksWithEmptyList() {
        Song songNoId = new Song(); // spotifyId = null
        Playlist playlist = buildPlaylist(List.of(songNoId));
        when(playlistRepository.findById(1L)).thenReturn(Optional.of(playlist));
        when(spotifyAuthPort.getTokenForUser(5L)).thenReturn("token");
        when(spotifyPlaylistPort.getCurrentUserId("token")).thenReturn("uid");
        when(spotifyPlaylistPort.createPlaylist(any(), any(), any(), any())).thenReturn("pid");
        when(playlistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(1L, 5L);

        verify(spotifyPlaylistPort).addTracksToPlaylist(eq("token"), eq("pid"), argThat(List::isEmpty));
    }

}
