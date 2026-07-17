package com.dnd.app.service.media;

import com.dnd.app.domain.enums.MediaOwnerType;
import com.dnd.app.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Класс MediaPolicyRegistry собирает все бины {@link MediaOwnerPolicy} в карту по типу владельца и
 * выдаёт нужную политику сервису. Дубликат политики на один тип — ошибка конфигурации (падение на
 * старте). Запрос политики для неизвестного/ещё не подключённого типа даёт 400 (а не 500).
 */
@Component
public class MediaPolicyRegistry {

    private final Map<MediaOwnerType, MediaOwnerPolicy> policies;

    /**
     * Строит реестр из всех найденных политик.
     * @param policyBeans список зарегистрированных политик (может быть пустым на ранних фазах)
     */
    public MediaPolicyRegistry(List<MediaOwnerPolicy> policyBeans) {
        Map<MediaOwnerType, MediaOwnerPolicy> map = new EnumMap<>(MediaOwnerType.class);
        for (MediaOwnerPolicy policy : policyBeans) {
            MediaOwnerPolicy previous = map.put(policy.type(), policy);
            if (previous != null) {
                throw new IllegalStateException("Дублирующая MediaOwnerPolicy для типа " + policy.type()
                        + ": " + previous.getClass().getName() + " и " + policy.getClass().getName());
            }
        }
        this.policies = map;
    }

    /**
     * Возвращает политику для типа владельца или бросает 400, если она не зарегистрирована.
     * @param type тип владельца ассета
     * @return политика прав для этого типа
     */
    public MediaOwnerPolicy require(MediaOwnerType type) {
        MediaOwnerPolicy policy = policies.get(type);
        if (policy == null) {
            throw new BadRequestException("Тип медиа-владельца пока не поддерживается: " + type);
        }
        return policy;
    }
}
