package es.wara.ecos.backend.infrastructure.persistence.mapper;

import es.wara.ecos.backend.domain.model.ExplorationSession;
import es.wara.ecos.backend.domain.model.SessionNode;
import es.wara.ecos.backend.infrastructure.persistence.entity.ExplorationSessionEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.SessionNodeEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.SongEntity;
import es.wara.ecos.backend.infrastructure.persistence.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper entre ExplorationSessionEntity y el modelo de dominio ExplorationSession.
 *
 * @author Wara Pacheco
 * @version 2.0
 * @since 1.0.0
 */
@Component
public class SessionMapper {

    @Autowired
    private SongMapper songMapper;

    public ExplorationSession toModel(ExplorationSessionEntity entity) {
        if (entity == null) return null;
        ExplorationSession session = new ExplorationSession();
        session.setId(entity.getId());
        session.setUserId(entity.getUser() != null ? entity.getUser().getId() : null);
        session.setStartSongId(entity.getStartSong() != null ? entity.getStartSong().getId() : null);
        session.setName(entity.getName());
        session.setCreatedAt(entity.getCreatedAt());
        if (entity.getNodes() != null) {
            session.setNodes(entity.getNodes().stream()
                    .map(this::toNodeModel)
                    .collect(Collectors.toCollection(ArrayList::new)));
        }
        return session;
    }

    public ExplorationSessionEntity toEntity(ExplorationSession model) {
        if (model == null) return null;
        ExplorationSessionEntity entity = new ExplorationSessionEntity();
        entity.setId(model.getId());
        entity.setName(model.getName());
        entity.setCreatedAt(model.getCreatedAt());
        if (model.getUserId() != null) {
            UserEntity user = new UserEntity();
            user.setId(model.getUserId());
            entity.setUser(user);
        }
        if (model.getStartSongId() != null) {
            SongEntity startSong = new SongEntity();
            startSong.setId(model.getStartSongId());
            entity.setStartSong(startSong);
        }
        if (model.getNodes() != null) {
            List<SessionNodeEntity> nodeEntities = model.getNodes().stream()
                    .map(node -> toNodeEntity(node, entity))
                    .toList();
            entity.getNodes().addAll(nodeEntities);
        }
        return entity;
    }

    private SessionNode toNodeModel(SessionNodeEntity entity) {
        SessionNode node = new SessionNode();
        node.setId(entity.getId());
        node.setSessionId(entity.getSession() != null ? entity.getSession().getId() : null);
        if (entity.getSong() != null) {
            node.setSong(songMapper.toModel(entity.getSong()));
        }
        node.setSequenceOrder(entity.getSequenceOrder() != null ? entity.getSequenceOrder().intValue() : null);
        return node;
    }

    private SessionNodeEntity toNodeEntity(SessionNode node, ExplorationSessionEntity sessionEntity) {
        SessionNodeEntity nodeEntity = new SessionNodeEntity();
        nodeEntity.setId(node.getId());
        nodeEntity.setSession(sessionEntity);
        if (node.getSong() != null) {
            SongEntity song = new SongEntity();
            song.setId(node.getSong().getId());
            nodeEntity.setSong(song);
        }
        nodeEntity.setSequenceOrder(node.getSequenceOrder() != null ? node.getSequenceOrder().shortValue() : (short) 1);
        return nodeEntity;
    }

}