package com.dnd.app.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Класс CharacterClassLevelId описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CharacterClassLevelId implements Serializable {
    private UUID characterId;
    private UUID classId;
}
