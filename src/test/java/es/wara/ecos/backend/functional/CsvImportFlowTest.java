package es.wara.ecos.backend.functional;

import es.wara.ecos.backend.domain.port.MusicProvider;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaAlbumRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaArtistRepository;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaSongRepository;
import es.wara.ecos.backend.infrastructure.seed.CsvImportRunner;
import es.wara.ecos.backend.support.BaseFunctionalTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests funcionales para CsvImportRunner.
 *
 * Verifica que la importación desde CSV crea álbumes, artistas y canciones
 * correctamente, incluyendo el fallback por título cuando album_spotify_id
 * está ausente.
 */
class CsvImportFlowTest extends BaseFunctionalTest {

    @MockitoBean
    MusicProvider musicProvider;

    @Autowired CsvImportRunner csvImportRunner;
    @Autowired JpaSongRepository songRepo;
    @Autowired JpaAlbumRepository albumRepo;
    @Autowired JpaArtistRepository artistRepo;
    @Autowired JdbcTemplate jdbc;

    // 22 columnas — mismo orden que CsvImportRunner espera
    private static final String HEADERS =
        "incomplete,spotify_id,artist_name,album_spotify_id,album_title,release_year," +
        "cover_url,title,duration_ms,preview_url,isrc," +
        "energy,tempo,valence,danceability,acousticness,instrumentalness,speechiness," +
        "loudness,musical_key,mode,genre_hint";

    @BeforeEach
    void cleanDb() {
        jdbc.execute("TRUNCATE TABLE songs, albums, artists CASCADE");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ÁLBUM SIN SPOTIFY ID
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void albumCreadoDesdeAlbumTitleCuandoNoHaySpotifyId() {
        String csv = HEADERS + "\n" +
            "False,spot1,Pink Floyd,,Dark Side of the Moon,1973,,Money,382000,," +
            ",0.8,120.0,0.5,0.7,0.1,0.0,0.05,-10.0,5,0,rock";

        int[] result = csvImportRunner.importFromReader(new StringReader(csv), 0, "test");

        assertThat(result[0]).as("canciones importadas").isEqualTo(1);
        assertThat(result[2]).as("errores").isEqualTo(0);
        assertThat(albumRepo.count()).isEqualTo(1);
        assertThat(albumRepo.findFirstByTitleIgnoreCase("Dark Side of the Moon")).isPresent();

        // Verificar que song.album_id quedó enlazado (sin acceder al proxy lazy)
        Integer songsConAlbum = jdbc.queryForObject(
            "SELECT COUNT(*) FROM songs WHERE spotify_id = 'spot1' AND album_id IS NOT NULL",
            Integer.class);
        assertThat(songsConAlbum).isEqualTo(1);
    }

    @Test
    void dosCancionesDelMismoAlbumCreanUnSoloAlbum() {
        String csv = HEADERS + "\n" +
            "False,spot1,Pink Floyd,,Dark Side of the Moon,1973,,Money,382000,," +
            ",0.8,120.0,0.5,0.7,0.1,0.0,0.05,-10.0,5,0,rock\n" +
            "False,spot2,Pink Floyd,,Dark Side of the Moon,1973,,Time,421000,," +
            ",0.7,136.0,0.4,0.6,0.1,0.0,0.04,-11.0,5,0,rock";

        int[] result = csvImportRunner.importFromReader(new StringReader(csv), 0, "test");

        assertThat(result[0]).as("canciones importadas").isEqualTo(2);
        assertThat(albumRepo.count()).as("álbumes — solo uno creado por dedup de título").isEqualTo(1);
        assertThat(artistRepo.count()).as("artistas — solo uno creado por dedup de nombre").isEqualTo(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ÁLBUM CON SPOTIFY ID
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void albumCreadoConSpotifyIdCuandoEstaPresente() {
        String csv = HEADERS + "\n" +
            "False,spot1,Pink Floyd,album_spt_abc,Dark Side of the Moon,1973,,Money,382000,," +
            ",0.8,120.0,0.5,0.7,0.1,0.0,0.05,-10.0,5,0,rock";

        int[] result = csvImportRunner.importFromReader(new StringReader(csv), 0, "test");

        assertThat(result[0]).isEqualTo(1);
        var album = albumRepo.findBySpotifyId("album_spt_abc");
        assertThat(album).isPresent();
        assertThat(album.get().getTitle()).isEqualTo("Dark Side of the Moon");
        assertThat(album.get().getYear()).isEqualTo((short) 1973);
    }

    @Test
    void dosCancionesDelMismoAlbumSpotifyIdCreanUnSoloAlbum() {
        String csv = HEADERS + "\n" +
            "False,spot1,Radiohead,album_ok_computer,OK Computer,1997,,Paranoid Android,383000,," +
            ",0.5,80.0,0.1,0.3,0.7,0.1,0.06,-14.0,4,0,alternative\n" +
            "False,spot2,Radiohead,album_ok_computer,OK Computer,1997,,Karma Police,264000,," +
            ",0.4,76.0,0.2,0.4,0.6,0.0,0.05,-15.0,4,0,alternative";

        int[] result = csvImportRunner.importFromReader(new StringReader(csv), 0, "test");

        assertThat(result[0]).isEqualTo(2);
        assertThat(albumRepo.count()).as("álbum dedup por spotify_id").isEqualTo(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ALFABETOS NO LATINOS
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void cancionesConTitulosEnAlfabetoNoLatino() {
        String csv = HEADERS + "\n" +
            "False,spot_jp,アーティスト,,アルバム,2020,,曲名,250000,," +
            ",0.6,130.0,0.5,0.6,0.2,0.0,0.03,-9.0,2,0,j-pop\n" +
            "False,spot_ar,فنان,,ألبوم,2021,,أغنية,300000,," +
            ",0.7,95.0,0.6,0.5,0.3,0.0,0.02,-12.0,0,1,arabic\n" +
            "False,spot_kr,아티스트,,앨범,2022,,노래,210000,," +
            ",0.8,110.0,0.7,0.7,0.1,0.0,0.04,-8.0,1,0,k-pop";

        int[] result = csvImportRunner.importFromReader(new StringReader(csv), 0, "test");

        assertThat(result[0]).as("todas las canciones importadas").isEqualTo(3);
        assertThat(result[2]).as("sin errores").isEqualTo(0);
        assertThat(albumRepo.count()).isEqualTo(3);
        assertThat(albumRepo.findFirstByTitleIgnoreCase("アルバム")).isPresent();
        assertThat(albumRepo.findFirstByTitleIgnoreCase("ألبوم")).isPresent();
        assertThat(albumRepo.findFirstByTitleIgnoreCase("앨범")).isPresent();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CASOS BORDE
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void filaConIncompleteTrueSeOmite() {
        String csv = HEADERS + "\n" +
            "True,spot1,Pink Floyd,,Dark Side of the Moon,1973,,Money,382000,," +
            ",0.8,120.0,0.5,0.7,0.1,0.0,0.05,-10.0,5,0,rock";

        int[] result = csvImportRunner.importFromReader(new StringReader(csv), 0, "test");

        assertThat(result[0]).as("importadas").isEqualTo(0);
        assertThat(result[1]).as("omitidas").isEqualTo(1);
        assertThat(songRepo.count()).isEqualTo(0);
        assertThat(albumRepo.count()).isEqualTo(0);
    }

    @Test
    void cancionDuplicadaSeOmiteSinError() {
        String csv = HEADERS + "\n" +
            "False,spot_dup,Pink Floyd,,Dark Side of the Moon,1973,,Money,382000,," +
            ",0.8,120.0,0.5,0.7,0.1,0.0,0.05,-10.0,5,0,rock";

        csvImportRunner.importFromReader(new StringReader(csv), 0, "first");
        int[] second = csvImportRunner.importFromReader(new StringReader(csv), 0, "second");

        assertThat(second[0]).as("importadas en segunda pasada").isEqualTo(0);
        assertThat(second[1]).as("omitidas por spotifyId duplicado").isEqualTo(1);
        assertThat(second[2]).as("errores").isEqualTo(0);
        assertThat(songRepo.count()).isEqualTo(1);
        assertThat(albumRepo.count()).isEqualTo(1);
    }

    @Test
    void sinAlbumTitleNiSpotifyIdLaCancionSeGuardaSinAlbum() {
        String csv = HEADERS + "\n" +
            "False,spot_noalbum,Artista sin album,,,,,Canción suelta,180000,," +
            ",0.5,100.0,0.5,0.5,0.3,0.0,0.05,-12.0,0,0,pop";

        int[] result = csvImportRunner.importFromReader(new StringReader(csv), 0, "test");

        assertThat(result[0]).isEqualTo(1);
        assertThat(albumRepo.count()).as("no se crea álbum sin título").isEqualTo(0);
        Integer songsConAlbum = jdbc.queryForObject(
            "SELECT COUNT(*) FROM songs WHERE spotify_id = 'spot_noalbum' AND album_id IS NULL",
            Integer.class);
        assertThat(songsConAlbum).isEqualTo(1);
    }
}