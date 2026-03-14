package es.wara.ecos.backend.infrastructure.persistence.repository;

import es.wara.ecos.backend.domain.model.AudioFeatures;
import es.wara.ecos.backend.infrastructure.persistence.jpa.JpaAudioFeaturesRepository;
import es.wara.ecos.backend.infrastructure.persistence.mapper.AudioFeaturesMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Componente interno de infraestructura para la gestión de audio_features; no implementa un puerto de dominio directamente.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Repository
public class AudioFeaturesRepositoryImpl {

    private final JpaAudioFeaturesRepository jpa;
    private final AudioFeaturesMapper mapper;

    public AudioFeaturesRepositoryImpl(JpaAudioFeaturesRepository jpa, AudioFeaturesMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    public Optional<AudioFeatures> findBySongId(UUID songId) {
        return jpa.findById(songId).map(mapper::toModel);
    }

    public AudioFeatures save(AudioFeatures audioFeatures) {
        return mapper.toModel(jpa.save(mapper.toEntity(audioFeatures)));
    }

}
