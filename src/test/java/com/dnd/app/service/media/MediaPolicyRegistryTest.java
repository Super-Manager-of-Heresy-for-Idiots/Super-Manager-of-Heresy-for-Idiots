package com.dnd.app.service.media;

import com.dnd.app.domain.enums.MediaOwnerType;
import com.dnd.app.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("MediaPolicyRegistry: разрешение политик по типу владельца")
class MediaPolicyRegistryTest {

    @Test
    @DisplayName("Возвращает политику для зарегистрированного типа")
    void require_returnsRegisteredPolicy() {
        MediaOwnerPolicy policy = policyFor(MediaOwnerType.CHARACTER_AVATAR);
        MediaPolicyRegistry registry = new MediaPolicyRegistry(List.of(policy));

        assertSame(policy, registry.require(MediaOwnerType.CHARACTER_AVATAR));
    }

    @Test
    @DisplayName("Неизвестный/неподключённый тип → 400 (BadRequestException)")
    void require_unknownType_throws400() {
        MediaPolicyRegistry registry = new MediaPolicyRegistry(List.of());

        assertThrows(BadRequestException.class, () -> registry.require(MediaOwnerType.CHARACTER_AVATAR));
    }

    @Test
    @DisplayName("Две политики на один тип — ошибка конфигурации на старте")
    void constructor_duplicatePolicies_throws() {
        MediaOwnerPolicy first = policyFor(MediaOwnerType.CHARACTER_AVATAR);
        MediaOwnerPolicy second = policyFor(MediaOwnerType.CHARACTER_AVATAR);

        assertThrows(IllegalStateException.class, () -> new MediaPolicyRegistry(List.of(first, second)));
    }

    /**
     * Создаёт минимальную политику-заглушку для заданного типа владельца.
     * @param type тип владельца, который «обслуживает» заглушка
     * @return политика, отвечающая только за {@link MediaOwnerPolicy#type()}
     */
    private MediaOwnerPolicy policyFor(MediaOwnerType type) {
        return new MediaOwnerPolicy() {
            @Override
            public MediaOwnerType type() {
                return type;
            }

            @Override
            public void checkUpload(UUID ownerId, MediaUser user) {
                // no-op заглушка
            }

            @Override
            public void checkRead(UUID ownerId, MediaUser user) {
                // no-op заглушка
            }
        };
    }
}
