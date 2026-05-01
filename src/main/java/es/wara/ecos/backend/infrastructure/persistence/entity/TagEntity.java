package es.wara.ecos.backend.infrastructure.persistence.entity;

/**
 * Clase vacía reservada por convención de nomenclatura.
 * <p>
 * No existe una tabla {@code tags} independiente en el esquema.
 * Los tags de canciones se almacenan directamente en {@code song_tags}
 * como columna de texto y están mapeados por {@link SongTagEntity}
 * usando la clave compuesta {@link SongTagId}.
 * </p>
 *
 * @author Wara Pacheco
 * @version 1.0
 * @since 1.0.0
 */
public class TagEntity {
}