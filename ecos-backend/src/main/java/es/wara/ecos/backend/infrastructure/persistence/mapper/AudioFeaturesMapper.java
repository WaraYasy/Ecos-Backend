package es.wara.ecos.backend.infrastructure.persistence.mapper;

import es.wara.ecos.backend.domain.model.AudioFeatures;
import es.wara.ecos.backend.infrastructure.persistence.entity.AudioFeaturesEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper que convierte entre AudioFeaturesEntity y el modelo de dominio AudioFeatures.
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
@Component
public class AudioFeaturesMapper {

    public AudioFeatures toModel(AudioFeaturesEntity entity) {
        if (entity == null) return null;
        return new AudioFeatures(
                entity.getSongId(),
                entity.getEnergy(),
                entity.getTempo(),
                entity.getValence(),
                entity.getDanceability(),
                entity.getAcousticness(),
                entity.getInstrumentalness(),
                entity.getSpeechiness(),
                entity.getLoudness(),
                entity.getMusicalKey() != null ? entity.getMusicalKey().intValue() : null,
                entity.getMode() != null ? entity.getMode().intValue() : null,
                entity.getTimeSignature() != null ? entity.getTimeSignature().intValue() : null
        );
    }

    public AudioFeaturesEntity toEntity(AudioFeatures model) {
        if (model == null) return null;
        AudioFeaturesEntity entity = new AudioFeaturesEntity();
        entity.setSongId(model.getSongId());
        entity.setEnergy(model.getEnergy());
        entity.setTempo(model.getTempo());
        entity.setValence(model.getValence());
        entity.setDanceability(model.getDanceability());
        entity.setAcousticness(model.getAcousticness());
        entity.setInstrumentalness(model.getInstrumentalness());
        entity.setSpeechiness(model.getSpeechiness());
        entity.setLoudness(model.getLoudness());
        entity.setMusicalKey(model.getMusicalKey() != null ? model.getMusicalKey().shortValue() : null);
        entity.setMode(model.getMode() != null ? model.getMode().shortValue() : null);
        entity.setTimeSignature(model.getTimeSignature() != null ? model.getTimeSignature().shortValue() : null);
        return entity;
    }

}
