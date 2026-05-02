package es.wara.ecos.backend.infrastructure.adapter;

import es.wara.ecos.backend.domain.model.Song;
import es.wara.ecos.backend.domain.model.Tag;
import es.wara.ecos.backend.infrastructure.adapter.lastfm.LastFmClient;
import es.wara.ecos.backend.infrastructure.adapter.lastfm.dto.LastFmTagDTO;
import es.wara.ecos.backend.infrastructure.adapter.lastfm.dto.LastFmTrackInfoDTO;
import es.wara.ecos.backend.infrastructure.adapter.musicbrainz.MusicBrainzClient;
import es.wara.ecos.backend.infrastructure.adapter.spotify.SpotifyClient;
import es.wara.ecos.backend.infrastructure.adapter.tmdb.TmdbClient;
import es.wara.ecos.backend.infrastructure.adapter.itunes.ItunesClient;
import es.wara.ecos.backend.infrastructure.seed.SeedTermsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagClassificationTest {

    @Mock SpotifyClient spotifyClient;
    @Mock LastFmClient lastFmClient;
    @Mock TmdbClient tmdbClient;
    @Mock MusicBrainzClient musicBrainzClient;
    @Mock ItunesClient itunesClient;
    @Mock SeedTermsProperties seedTerms;

    MusicProviderAdapter adapter;

    @BeforeEach
    void setUp() {
        lenient().when(seedTerms.getGenres()).thenReturn(List.of("rock", "jazz", "indie pop"));
        lenient().when(seedTerms.getAesthetics()).thenReturn(List.of("dark academia", "cottagecore"));
        adapter = new MusicProviderAdapter(spotifyClient, lastFmClient, tmdbClient,
                musicBrainzClient, itunesClient, seedTerms);
    }

    @Test
    void getSongTags_tagInGenreList_classifiedAsGenre() {
        when(lastFmClient.getTrackTags("Radiohead", "Creep")).thenReturn(trackInfoWith("rock", 80));

        List<Tag> tags = adapter.getSongTags("Radiohead", "Creep");

        assertThat(tags).hasSize(1);
        assertThat(tags.get(0).getType()).isEqualTo("genre");
    }

    @Test
    void getSongTags_tagInAestheticList_classifiedAsAesthetic() {
        when(lastFmClient.getTrackTags("Various", "Song")).thenReturn(trackInfoWith("dark academia", 70));

        List<Tag> tags = adapter.getSongTags("Various", "Song");

        assertThat(tags).hasSize(1);
        assertThat(tags.get(0).getType()).isEqualTo("aesthetic");
    }

    @Test
    void getSongTags_unknownTag_classifiedAsOther() {
        when(lastFmClient.getTrackTags("Various", "Song")).thenReturn(trackInfoWith("melancholic", 60));

        List<Tag> tags = adapter.getSongTags("Various", "Song");

        assertThat(tags).hasSize(1);
        assertThat(tags.get(0).getType()).isEqualTo("other");
    }

    @Test
    void getSongTags_tagNormalizedToLowercase_matchesCorrectly() {
        when(lastFmClient.getTrackTags("Artist", "Track")).thenReturn(trackInfoWith("Indie Pop", 50));

        List<Tag> tags = adapter.getSongTags("Artist", "Track");

        assertThat(tags.get(0).getType()).isEqualTo("genre");
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private LastFmTrackInfoDTO trackInfoWith(String tagName, int count) {
        LastFmTagDTO tagDTO = new LastFmTagDTO();
        tagDTO.setName(tagName);
        tagDTO.setCount(count);

        LastFmTrackInfoDTO.TopTagsWrapper topTags = new LastFmTrackInfoDTO.TopTagsWrapper();
        topTags.setTag(List.of(tagDTO));

        LastFmTrackInfoDTO info = new LastFmTrackInfoDTO();
        info.setToptags(topTags);  // getter/setter generado por Lombok como getToptags/setToptags
        return info;
    }
}