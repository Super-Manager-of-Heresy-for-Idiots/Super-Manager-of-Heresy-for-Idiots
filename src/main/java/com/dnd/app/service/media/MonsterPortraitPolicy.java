package com.dnd.app.service.media;

import com.dnd.app.domain.enums.MediaOwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Класс MonsterPortraitPolicy — политика прав на портрет монстра ({@code MONSTER_PORTRAIT}).
 * Делегирует scope-aware проверку в {@link MonsterMediaAccess} (SYSTEM→ADMIN, HOMEBREW→автор,
 * CAMPAIGN→GM для записи; чтение — по видимости монстра).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "minio.endpoint")
public class MonsterPortraitPolicy implements MediaOwnerPolicy {

    private final MonsterMediaAccess monsterMediaAccess;

    /** @return тип владельца — портрет монстра */
    @Override
    public MediaOwnerType type() {
        return MediaOwnerType.MONSTER_PORTRAIT;
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
