package com.dnd.app.domain.content;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

/**
 * Класс MagicItemAllowedEquipmentId описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MagicItemAllowedEquipmentId implements Serializable {

    private UUID magicItem;
    private UUID equipmentItem;
}
