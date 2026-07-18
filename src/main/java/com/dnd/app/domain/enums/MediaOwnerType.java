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
    BLUEPRINT_COVER(MediaLimitCategory.COVER),

    /** Превью локации кампании (owner_id = campaign_locations.id). */
    LOCATION_PREVIEW(MediaLimitCategory.COVER),

    /** Портрет монстра бестиария (owner_id = monsters.id). */
    MONSTER_PORTRAIT(MediaLimitCategory.AVATAR),

    /** Токен монстра для карты боя (owner_id = monsters.id); браузер грузит его как обычный media. */
    MONSTER_TOKEN(MediaLimitCategory.AVATAR),

    /** Аватар пользователя (owner_id = users.id). */
    USER_AVATAR(MediaLimitCategory.AVATAR),

    /** Обложка кампании (owner_id = campaigns.id). */
    CAMPAIGN_COVER(MediaLimitCategory.COVER),

    /** Арт квеста кампании (owner_id = campaign_quests.id). */
    QUEST_ART(MediaLimitCategory.COVER),

    /** Арт заклинания (owner_id = spell.spell_id). */
    SPELL_ART(MediaLimitCategory.COVER);

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
