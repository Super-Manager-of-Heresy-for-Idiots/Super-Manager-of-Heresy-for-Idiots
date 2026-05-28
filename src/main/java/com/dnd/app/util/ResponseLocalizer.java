package com.dnd.app.util;

import com.dnd.app.domain.enums.EquipmentSlot;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.domain.enums.Rarity;
import com.dnd.app.domain.enums.Role;

public final class ResponseLocalizer {

    private ResponseLocalizer() {
    }

    public static String role(Role role) {
        if (role == null) return null;
        return switch (role) {
            case PLAYER -> "Игрок";
            case GAME_MASTER -> "Мастер игры";
            case ADMIN -> "Администратор";
        };
    }

    public static String equipmentSlot(EquipmentSlot slot) {
        if (slot == null) return null;
        return switch (slot) {
            case HEAD -> "Голова";
            case CHEST -> "Торс";
            case LEGS -> "Ноги";
            case FEET -> "Ступни";
            case MAIN_HAND -> "Основная рука";
            case OFF_HAND -> "Вторая рука";
            case RING_LEFT -> "Левое кольцо";
            case RING_RIGHT -> "Правое кольцо";
            case NECK -> "Шея";
            case CLOAK -> "Плащ";
        };
    }

    public static String rarity(Rarity rarity) {
        if (rarity == null) return null;
        return switch (rarity) {
            case COMMON -> "Обычный";
            case UNCOMMON -> "Необычный";
            case RARE -> "Редкий";
            case VERY_RARE -> "Очень редкий";
            case LEGENDARY -> "Легендарный";
        };
    }

    public static String homebrewStatus(HomebrewStatus status) {
        if (status == null) return null;
        return switch (status) {
            case DRAFT -> "Черновик";
            case PUBLISHED -> "Опубликован";
            case UNPUBLISHED -> "Снят с публикации";
        };
    }

    public static String contentType(String contentType) {
        if (contentType == null) return null;
        return switch (contentType) {
            case "ITEM_TYPE" -> "Тип предмета";
            case "CHARACTER_CLASS" -> "Класс персонажа";
            case "SKILL" -> "Умение";
            case "FEAT" -> "Черта";
            default -> contentType;
        };
    }

    public static String rewardType(String rewardType) {
        if (rewardType == null) return null;
        return switch (rewardType) {
            case "SKILL" -> "Умение";
            case "SUBCLASS" -> "Подкласс";
            case "FEAT" -> "Черта";
            default -> rewardType;
        };
    }

    public static String skillType(String skillType) {
        if (skillType == null) return null;
        return switch (skillType) {
            case "COMBAT" -> "Боевой";
            case "PASSIVE" -> "Пассивный";
            case "UTILITY" -> "Вспомогательный";
            default -> skillType;
        };
    }

    public static String deletedTitle(String title) {
        return "[УДАЛЕНО] " + title;
    }
}
