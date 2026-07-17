package com.dnd.app.service.media;

import com.dnd.app.domain.enums.MediaOwnerType;

import java.util.UUID;

/**
 * Интерфейс MediaOwnerPolicy — SPI прав доступа для одного типа владельца медиа-ассета.
 * На каждый {@link MediaOwnerType} регистрируется ровно одна политика-бин; {@link MediaPolicyRegistry}
 * находит её по {@link #type()}. Политики пишутся по мере внедрения типов (фазы 1–4) и переиспользуют
 * уже существующие проверки доступа домена, а не дублируют их. Методы бросают доменные исключения
 * (404/403), если доступ запрещён.
 */
public interface MediaOwnerPolicy {

    /** @return тип владельца, который обслуживает эта политика */
    MediaOwnerType type();

    /**
     * Проверяет право пользователя загружать/заменять/удалять ассет данного владельца.
     * @param ownerId идентификатор владельца ассета
     * @param user текущий пользователь
     */
    void checkUpload(UUID ownerId, MediaUser user);

    /**
     * Проверяет право пользователя читать (просматривать) ассет данного владельца.
     * @param ownerId идентификатор владельца ассета
     * @param user текущий пользователь
     */
    void checkRead(UUID ownerId, MediaUser user);

    /**
     * Опционально проверяет существование владельца до загрузки (чтобы не создавать ассет-сироту
     * для несуществующей сущности). По умолчанию — no-op: проверка совмещена с {@link #checkUpload}.
     * @param ownerId идентификатор проверяемого владельца
     */
    default void validateOwnerExists(UUID ownerId) {
        // по умолчанию отдельная проверка не требуется — checkUpload сам находит и валидирует владельца
    }
}
