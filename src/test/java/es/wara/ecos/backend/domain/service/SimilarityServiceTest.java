package es.wara.ecos.backend.domain.service;

import es.wara.ecos.backend.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SimilarityServiceTest {

    SimilarityService service;

    @BeforeEach
    void setUp() {
        service = new SimilarityService();
    }

    // ─── audioSimilarity ─────────────────────────────────────────────────────

    @Test
    void audioSimilarity_identicalFeatures_returns1() {
        AudioFeatures af = audioFeatures("0.8", "120", "0.6", "0.7", "0.1", "0.0", "0.05", "-5");

        float result = service.audioSimilarity(af, af);

        assertThat(result).isCloseTo(1.0f, within(0.001f));
    }

    @Test
    void audioSimilarity_nullFeatures_returns0() {
        assertThat(service.audioSimilarity(null, null)).isEqualTo(0f);
        assertThat(service.audioSimilarity(audioFeatures("0.5", "120", "0.5", "0.5", "0.5", "0.0", "0.0", "-10"), null)).isEqualTo(0f);
    }

    @Test
    void audioSimilarity_differentFeatures_returns0to1() {
        AudioFeatures a = audioFeatures("1.0", "120", "1.0", "1.0", "0.0", "0.0", "0.0", "0");
        AudioFeatures b = audioFeatures("0.0", "40",  "0.0", "0.0", "1.0", "1.0", "1.0", "-60");

        float result = service.audioSimilarity(a, b);

        assertThat(result).isBetween(0f, 1f);
    }

    // ─── toNormalizedVector ───────────────────────────────────────────────────

    @Test
    void toNormalizedVector_allValues_returnsLength8() {
        AudioFeatures af = audioFeatures("0.8", "120", "0.6", "0.7", "0.1", "0.0", "0.05", "-5");

        float[] vec = af.toNormalizedVector();

        assertThat(vec).hasSize(8);
    }

    @Test
    void toNormalizedVector_tempo120_normalizesCorrectly() {
        // tempo_norm = (120 - 40) / 180 = 0.444
        AudioFeatures af = audioFeatures("0.5", "120", "0.5", "0.5", "0.5", "0.0", "0.0", "-30");

        float[] vec = af.toNormalizedVector();

        assertThat(vec[1]).isCloseTo(0.444f, within(0.01f));
    }

    @Test
    void toNormalizedVector_loudnessMinus30_normalizesCorrectly() {
        // loudness_norm = (-30 + 60) / 60 = 0.5
        AudioFeatures af = audioFeatures("0.5", "120", "0.5", "0.5", "0.5", "0.0", "0.0", "-30");

        float[] vec = af.toNormalizedVector();

        assertThat(vec[7]).isCloseTo(0.5f, within(0.001f));
    }

    // ─── tagSimilarity ────────────────────────────────────────────────────────

    @Test
    void tagSimilarity_identicalTags_returns1() {
        List<Tag> tags = List.of(new Tag("rock", BigDecimal.ONE), new Tag("indie", BigDecimal.ONE));

        assertThat(service.tagSimilarity(tags, tags)).isCloseTo(1.0f, within(0.001f));
    }

    @Test
    void tagSimilarity_noOverlap_returns0() {
        List<Tag> a = List.of(new Tag("rock", BigDecimal.ONE));
        List<Tag> b = List.of(new Tag("jazz", BigDecimal.ONE));

        assertThat(service.tagSimilarity(a, b)).isEqualTo(0f);
    }

    @Test
    void tagSimilarity_halfOverlap_returnsOneThird() {
        // A={rock, indie}, B={rock, pop} → intersection=1, union=3 → 1/3
        List<Tag> a = List.of(new Tag("rock", BigDecimal.ONE), new Tag("indie", BigDecimal.ONE));
        List<Tag> b = List.of(new Tag("rock", BigDecimal.ONE), new Tag("pop", BigDecimal.ONE));

        assertThat(service.tagSimilarity(a, b)).isCloseTo(1f / 3f, within(0.001f));
    }

    @Test
    void tagSimilarity_nullOrEmpty_returns0() {
        assertThat(service.tagSimilarity(null, null)).isEqualTo(0f);
        assertThat(service.tagSimilarity(List.of(), List.of())).isEqualTo(0f);
    }

    // ─── eraProximity ─────────────────────────────────────────────────────────

    @Test
    void eraProximity_sameYear_returns1() {
        assertThat(service.eraProximity(1990, 1990)).isCloseTo(1.0f, within(0.001f));
    }

    @Test
    void eraProximity_nullYears_returns0() {
        assertThat(service.eraProximity(null, 1990)).isEqualTo(0f);
        assertThat(service.eraProximity(1990, null)).isEqualTo(0f);
    }

    @Test
    void eraProximity_15yearsApart_returnsAbout0607() {
        // exp(-15² / (2 * 15²)) = exp(-0.5) ≈ 0.607
        float result = service.eraProximity(1990, 2005);

        assertThat(result).isCloseTo(0.607f, within(0.01f));
    }

    @Test
    void eraProximity_30yearsApart_returnsLow() {
        float result = service.eraProximity(1970, 2000);

        assertThat(result).isLessThan(0.2f);
    }

    // ─── movieOverlap ─────────────────────────────────────────────────────────

    @Test
    void movieOverlap_sameMovies_returns1() {
        UUID movieId = UUID.randomUUID();
        Movie m = movie(movieId);

        assertThat(service.movieOverlap(List.of(m), List.of(m))).isCloseTo(1.0f, within(0.001f));
    }

    @Test
    void movieOverlap_noOverlap_returns0() {
        assertThat(service.movieOverlap(List.of(movie(UUID.randomUUID())), List.of(movie(UUID.randomUUID())))).isEqualTo(0f);
    }

    @Test
    void movieOverlap_empty_returns0() {
        assertThat(service.movieOverlap(List.of(), List.of())).isEqualTo(0f);
        assertThat(service.movieOverlap(null, null)).isEqualTo(0f);
    }

    // ─── compute ─────────────────────────────────────────────────────────────

    @Test
    void compute_identicalSong_returnsNear1() {
        // Con movies vacías: audio(1.0)*0.35 + semantic(1.0)*0.25 + tag(1.0)*0.20
        //                   + era(1.0)*0.10 + movie(0.0)*0.10 = 0.90
        // movieOverlap devuelve 0 cuando ambas listas están vacías (sin contexto fílmico).
        Song song = songWith(audioFeatures("0.8", "120", "0.6", "0.7", "0.1", "0.0", "0.05", "-5"),
                             List.of(new Tag("rock", BigDecimal.ONE)),
                             1990);
        float[] emb = uniformEmbedding(384, 0.1f);

        float result = service.compute(song, song, emb, emb);

        assertThat(result).isGreaterThan(0.85f);
    }

    @Test
    void compute_withoutEmbeddings_weightsRedistributed_returnsBetween0and1() {
        Song a = songWith(audioFeatures("0.8", "120", "0.6", "0.7", "0.1", "0.0", "0.05", "-5"),
                          List.of(new Tag("rock", BigDecimal.ONE)), 1990);
        Song b = songWith(audioFeatures("0.4", "90", "0.3", "0.5", "0.3", "0.1", "0.02", "-10"),
                          List.of(new Tag("pop", BigDecimal.ONE)), 2005);

        float result = service.compute(a, b, null, null);

        assertThat(result).isBetween(0f, 1f);
    }

    // ─── dominantEdgeType ─────────────────────────────────────────────────────

    @Test
    void dominantEdgeType_audioHighest_returnsAudio() {
        GraphEdge.EdgeType type = service.dominantEdgeType(0.9f, 0.1f, 0.1f, 0.1f, 0.1f);

        assertThat(type).isEqualTo(GraphEdge.EdgeType.AUDIO);
    }

    @Test
    void dominantEdgeType_tagHighest_returnsTag() {
        GraphEdge.EdgeType type = service.dominantEdgeType(0.1f, 0.1f, 0.9f, 0.1f, 0.1f);

        assertThat(type).isEqualTo(GraphEdge.EdgeType.TAG);
    }

    @Test
    void dominantEdgeType_eraHighest_returnsEra() {
        // era weight 0.10 * 1.0 = 0.10 vs audio 0.35 * 0.2 = 0.07
        GraphEdge.EdgeType type = service.dominantEdgeType(0.2f, 0.0f, 0.2f, 1.0f, 0.0f);

        assertThat(type).isEqualTo(GraphEdge.EdgeType.ERA);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private AudioFeatures audioFeatures(String energy, String tempo, String valence,
                                        String danceability, String acousticness,
                                        String instrumentalness, String speechiness,
                                        String loudness) {
        AudioFeatures af = new AudioFeatures();
        af.setEnergy(new BigDecimal(energy));
        af.setTempo(new BigDecimal(tempo));
        af.setValence(new BigDecimal(valence));
        af.setDanceability(new BigDecimal(danceability));
        af.setAcousticness(new BigDecimal(acousticness));
        af.setInstrumentalness(new BigDecimal(instrumentalness));
        af.setSpeechiness(new BigDecimal(speechiness));
        af.setLoudness(new BigDecimal(loudness));
        return af;
    }

    private Song songWith(AudioFeatures af, List<Tag> tags, Integer year) {
        Song s = new Song();
        s.setId(UUID.randomUUID());
        s.setAudioFeatures(af);
        s.setTags(tags);
        if (year != null) {
            Album album = new Album();
            album.setYear(year);
            s.setAlbum(album);
        }
        s.setMovies(List.of());
        s.setArtists(List.of());
        return s;
    }

    private Movie movie(UUID id) {
        Movie m = new Movie();
        m.setId(id);
        return m;
    }

    private float[] uniformEmbedding(int size, float value) {
        float[] emb = new float[size];
        for (int i = 0; i < size; i++) emb[i] = value;
        return emb;
    }

}