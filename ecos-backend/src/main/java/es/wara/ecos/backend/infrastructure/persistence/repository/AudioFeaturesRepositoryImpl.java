package es.wara.ecos.backend.infrastructure.persistence.repository;

import es.wara.ecos.backend.domain.model.AudioFeatures;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaAudioFeaturesRepository;
import es.wara.ecos.backend.infrastructure.persistence.mapper.AudioFeaturesMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AudioFeaturesRepositoryImpl {

    private final JpaAudioFeaturesRepository jpa;
    private final AudioFeaturesMapper mapper;

    public AudioFeaturesRepositoryImpl(JpaAudioFeaturesRepository jpa, AudioFeaturesMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    public Optional<AudioFeatures> findBySongId(Long songId) {
        return jpa.findById(songId).map(mapper::toModel);
    }

    public AudioFeatures save(AudioFeatures audioFeatures) {
        return mapper.toModel(jpa.save(mapper.toEntity(audioFeatures)));
    }

}