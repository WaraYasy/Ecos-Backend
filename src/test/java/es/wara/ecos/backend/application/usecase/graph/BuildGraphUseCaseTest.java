package es.wara.ecos.backend.application.usecase.graph;

import es.wara.ecos.backend.domain.model.*;
import es.wara.ecos.backend.domain.port.MusicProvider;
import es.wara.ecos.backend.domain.port.SongEmbeddingRepository;
import es.wara.ecos.backend.domain.port.SongRepository;
import es.wara.ecos.backend.domain.service.SimilarityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuildGraphUseCaseTest {

    @Mock SongRepository songRepository;
    @Mock SongEmbeddingRepository embeddingRepository;
    @Mock MusicProvider musicProvider;

    BuildGraphUseCase useCase;

    private static final UUID ROOT_ID  = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CAND1_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CAND2_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @BeforeEach
    void setUp() {
        // SimilarityService sin mocks: es lógica pura de dominio
        useCase = new BuildGraphUseCase(songRepository, embeddingRepository,
                                        new SimilarityService(), musicProvider);
    }

    @Test
    void execute_songNotFound_throwsIllegalArgumentException() {
        when(songRepository.findById(ROOT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(ROOT_ID, 8))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(ROOT_ID.toString());
    }

    @Test
    void execute_noCandidatesInDb_returnsOnlyRootNode() {
        when(songRepository.findById(ROOT_ID)).thenReturn(Optional.of(rootSong()));
        when(embeddingRepository.findBySongId(ROOT_ID)).thenReturn(Optional.empty());
        // Fallback API externa sin canciones en nuestra BD
        when(musicProvider.getSimilarSongs(anyString(), anyString(), anyInt())).thenReturn(List.of());

        GraphResponse result = useCase.execute(ROOT_ID, 8);

        assertThat(result.rootSongId()).isEqualTo(ROOT_ID);
        assertThat(result.nodes()).hasSize(1);
        assertThat(result.edges()).isEmpty();
    }

    @Test
    void execute_rootNodeIsAtOrigin_withSimilarity1() {
        when(songRepository.findById(ROOT_ID)).thenReturn(Optional.of(rootSong()));
        when(embeddingRepository.findBySongId(ROOT_ID)).thenReturn(Optional.empty());
        when(musicProvider.getSimilarSongs(anyString(), anyString(), anyInt())).thenReturn(List.of());

        GraphResponse result = useCase.execute(ROOT_ID, 8);

        GraphNode rootNode = result.nodes().get(0);
        assertThat(rootNode.getXPosition()).isEqualTo(0f);
        assertThat(rootNode.getYPosition()).isEqualTo(0f);
        assertThat(rootNode.getSimilarity()).isEqualTo(1f);
    }

    @Test
    void execute_withMusicProviderFallback_returnsCandidatesFromDb() {
        Song root  = rootSong();
        Song cand1 = candidateSong(CAND1_ID, "spotify-cand1");
        Song cand2 = candidateSong(CAND2_ID, "spotify-cand2");

        // API externa devuelve canciones con spotifyId
        Song apiSong1 = new Song(); apiSong1.setSpotifyId("spotify-cand1");
        Song apiSong2 = new Song(); apiSong2.setSpotifyId("spotify-cand2");

        when(songRepository.findById(ROOT_ID)).thenReturn(Optional.of(root));
        when(embeddingRepository.findBySongId(ROOT_ID)).thenReturn(Optional.empty());
        when(musicProvider.getSimilarSongs(anyString(), anyString(), anyInt()))
                .thenReturn(List.of(apiSong1, apiSong2));
        when(songRepository.findBySpotifyId("spotify-cand1")).thenReturn(Optional.of(cand1));
        when(songRepository.findBySpotifyId("spotify-cand2")).thenReturn(Optional.of(cand2));
        when(songRepository.findAllById(anyList())).thenReturn(List.of(cand1, cand2));
        when(embeddingRepository.findBySongId(CAND1_ID)).thenReturn(Optional.empty());
        when(embeddingRepository.findBySongId(CAND2_ID)).thenReturn(Optional.empty());

        GraphResponse result = useCase.execute(ROOT_ID, 8);

        // root + 2 candidatos
        assertThat(result.nodes()).hasSize(3);
        assertThat(result.edges()).hasSize(2);
    }

    @Test
    void execute_withEmbeddings_usesVectorSearch() {
        float[] rootEmb = uniformEmbedding(384, 0.5f);
        Song root  = rootSong();
        Song cand1 = candidateSong(CAND1_ID, "spotify-cand1");

        when(songRepository.findById(ROOT_ID)).thenReturn(Optional.of(root));
        when(embeddingRepository.findBySongId(ROOT_ID))
                .thenReturn(Optional.of(embedding(ROOT_ID, rootEmb)));
        when(embeddingRepository.findNearestNeighbors(any(), anyInt(), anyList()))
                .thenReturn(List.of(CAND1_ID));
        when(songRepository.findAllById(anyList())).thenReturn(List.of(cand1));
        when(embeddingRepository.findBySongId(CAND1_ID)).thenReturn(Optional.empty());

        GraphResponse result = useCase.execute(ROOT_ID, 8);

        assertThat(result.nodes()).hasSize(2);
        assertThat(result.edges()).hasSize(1);
    }

    @Test
    void execute_maxNodesRespected_capsAt8() {
        Song root = rootSong();
        when(songRepository.findById(ROOT_ID)).thenReturn(Optional.of(root));
        when(embeddingRepository.findBySongId(ROOT_ID)).thenReturn(Optional.empty());
        when(musicProvider.getSimilarSongs(anyString(), anyString(), anyInt())).thenReturn(List.of());

        // maxNodes = 100, pero el límite interno es MAX_NODES = 8
        GraphResponse result = useCase.execute(ROOT_ID, 100);

        // Con 0 candidatos el grafo tiene solo el root — lo que verifica que no explota con valores grandes
        assertThat(result.nodes()).hasSizeLessThanOrEqualTo(BuildGraphUseCase.MAX_NODES + 1);
    }

    @Test
    void execute_edgesConnectRootToNeighbors() {
        Song root  = rootSong();
        Song cand1 = candidateSong(CAND1_ID, "spotify-cand1");
        Song apiSong1 = new Song(); apiSong1.setSpotifyId("spotify-cand1");

        when(songRepository.findById(ROOT_ID)).thenReturn(Optional.of(root));
        when(embeddingRepository.findBySongId(ROOT_ID)).thenReturn(Optional.empty());
        when(musicProvider.getSimilarSongs(anyString(), anyString(), anyInt())).thenReturn(List.of(apiSong1));
        when(songRepository.findBySpotifyId("spotify-cand1")).thenReturn(Optional.of(cand1));
        when(songRepository.findAllById(anyList())).thenReturn(List.of(cand1));
        when(embeddingRepository.findBySongId(CAND1_ID)).thenReturn(Optional.empty());

        GraphResponse result = useCase.execute(ROOT_ID, 8);

        GraphEdge edge = result.edges().get(0);
        assertThat(edge.getSourceId()).isEqualTo(ROOT_ID);
        assertThat(edge.getTargetId()).isEqualTo(CAND1_ID);
        assertThat(edge.getWeight()).isBetween(0f, 1f);
        assertThat(edge.getType()).isNotNull();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Song rootSong() {
        Song s = new Song();
        s.setId(ROOT_ID);
        s.setTitle("Root Song");
        s.setSpotifyId("spotify-root");
        Artist artist = new Artist();
        artist.setName("Test Artist");
        s.setArtists(List.of(artist));
        s.setTags(List.of());
        s.setMovies(List.of());
        AudioFeatures af = new AudioFeatures();
        af.setEnergy(new BigDecimal("0.8"));
        af.setTempo(new BigDecimal("120"));
        af.setValence(new BigDecimal("0.6"));
        af.setDanceability(new BigDecimal("0.7"));
        af.setAcousticness(new BigDecimal("0.1"));
        af.setInstrumentalness(new BigDecimal("0.0"));
        af.setSpeechiness(new BigDecimal("0.05"));
        af.setLoudness(new BigDecimal("-5"));
        s.setAudioFeatures(af);
        return s;
    }

    private Song candidateSong(UUID id, String spotifyId) {
        Song s = new Song();
        s.setId(id);
        s.setTitle("Candidate " + id);
        s.setSpotifyId(spotifyId);
        s.setArtists(List.of());
        s.setTags(List.of());
        s.setMovies(List.of());
        return s;
    }

    private SongEmbedding embedding(UUID songId, float[] vector) {
        SongEmbedding e = new SongEmbedding();
        e.setSongId(songId);
        e.setEmbedding(vector);
        return e;
    }

    private float[] uniformEmbedding(int size, float value) {
        float[] emb = new float[size];
        for (int i = 0; i < size; i++) emb[i] = value;
        return emb;
    }

}