package com.dnd.app.service.media;

import com.dnd.app.domain.enums.MediaOwnerType;
import com.dnd.app.security.CharacterAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Класс CharacterAvatarPolicy — политика прав на аватар персонажа ({@code CHARACTER_AVATAR}).
 * Загрузку/замену/удаление разрешает владельцу персонажа, GM его кампании или роли ADMIN — ровно эту
 * проверку уже инкапсулирует {@link CharacterAccessGuard#require}, поэтому переиспользуем её, а не
 * дублируем логику доступа. Чтение доступно любому аутентифицированному: аватары персонажей видны в
 * списках и на карточках участников кампании.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "minio.endpoint")
public class CharacterAvatarPolicy implements MediaOwnerPolicy {

    private final CharacterAccessGuard characterAccessGuard;

    /** @return тип владельца, обслуживаемый политикой — аватар персонажа */
    @Override
    public MediaOwnerType type() {
        return MediaOwnerType.CHARACTER_AVATAR;
    }

    /**
     * Разрешает загрузку/замену/удаление владельцу персонажа, GM его кампании или ADMIN.
     * @param ownerId идентификатор персонажа
     * @param user текущий пользователь
     */
    @Override
    public void checkUpload(UUID ownerId, MediaUser user) {
        characterAccessGuard.require(ownerId, user.username());
    }

    /**
     * Разрешает чтение любому аутентифицированному пользователю.
     * @param ownerId идентификатор персонажа
     * @param user текущий пользователь
     */
    @Override
    public void checkRead(UUID ownerId, MediaUser user) {
        // Аватары персонажей видны любому аутентифицированному пользователю — дополнительных проверок нет.
    }
}
