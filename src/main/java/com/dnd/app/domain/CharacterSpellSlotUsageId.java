package com.dnd.app.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Класс CharacterSpellSlotUsageId описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public class CharacterSpellSlotUsageId implements Serializable {

    private UUID characterId;
    private Integer spellLevel;

    /**
     * Создает экземпляр компонента домена и получает зависимости, необходимые для выполнения бизнес-логики.
     */
    public CharacterSpellSlotUsageId() {
    }

    /**
     * Создает экземпляр компонента домена и получает зависимости, необходимые для выполнения бизнес-логики.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param spellLevel входящее значение spell level, используемое бизнес-сценарием
     */
    public CharacterSpellSlotUsageId(UUID characterId, Integer spellLevel) {
        this.characterId = characterId;
        this.spellLevel = spellLevel;
    }

    /**
     * Выполняет операции "equals" в рамках бизнес-логики домена.
     * @param o входящее значение o, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CharacterSpellSlotUsageId that)) {
            return false;
        }
        return Objects.equals(characterId, that.characterId) && Objects.equals(spellLevel, that.spellLevel);
    }

    /**
     * Проверяет условие операции "hash code" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    @Override
    public int hashCode() {
        return Objects.hash(characterId, spellLevel);
    }
}
