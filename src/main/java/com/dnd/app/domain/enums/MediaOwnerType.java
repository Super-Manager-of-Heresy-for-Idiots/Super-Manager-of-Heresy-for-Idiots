package com.dnd.app.domain.enums;

/**
 * Перечисление MediaOwnerType описывает тип владельца медиа-ассета в полиморфной модели
 * {@code media_asset}. Значение хранится в колонке {@code owner_type} и определяет:
 * какая политика прав ({@code MediaOwnerPolicy}) обслуживает загрузку/чтение и какая
 * категория лимитов ({@link MediaLimitCategory}) применяется к файлу. Новые типы
 * добавляются в своих фазах внедрения без изменения ядра media-модуля.
 */
public enum MediaOwnerType {

    /** Аватар персонажа игрока (owner_id = characters.id). */
    CHARACTER_AVATAR(MediaLimitCategory.AVATAR),

    /** Портрет NPC кампании (owner_id = campaign_npcs.id). */
    NPC_PORTRAIT(MediaLimitCategory.AVATAR),

    /** Обложка homebrew-пакета для витрины (owner_id = homebrew_packages.id). */
    HOMEBREW_COVER(MediaLimitCategory.COVER),

    /** Обложка campaign blueprint (owner_id = campaign_blueprints.id). */
    BLUEPRINT_COVER(MediaLimitCategory.COVER);

    private final MediaLimitCategory limitCategory;

    /**
     * Создаёт тип владельца с привязкой к категории лимитов.
     * @param limitCategory категория лимитов размера/пикселей для этого типа
     */
    MediaOwnerType(MediaLimitCategory limitCategory) {
        this.limitCategory = limitCategory;
    }

    /** @return категория лимитов, применяемая к ассетам этого типа владельца */
    public MediaLimitCategory limitCategory() {
        return limitCategory;
    }
}
