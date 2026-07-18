package com.dnd.app.service.media;

import com.dnd.app.domain.enums.MediaOwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Класс MonsterTokenPolicy — политика прав на токен монстра для карты боя ({@code MONSTER_TOKEN}).
 * Права те же, что у портрета (через {@link MonsterMediaAccess}); токен браузер грузит как обычный
 * media-ассет ({@code /api/media/{id}/content}) — сервер map-service байты не тянет.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "minio.endpoint")
public class MonsterTokenPolicy implements MediaOwnerPolicy {

    private final MonsterMediaAccess monsterMediaAccess;

    /** @return тип владельца — токен монстра */
    @Override
    public MediaOwnerType type() {
        return MediaOwnerType.MONSTER_TOKEN;
    }

    @Override
    public void checkUpload(UUID ownerId, MediaUser user) {
        monsterMediaAccess.checkEdit(ownerId, user);
    }

    @Override
    public void checkRead(UUID ownerId, MediaUser user) {
        monsterMediaAccess.checkView(ownerId, user);
    }
}
