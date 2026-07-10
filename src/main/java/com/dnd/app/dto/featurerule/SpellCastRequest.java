package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс SpellCastRequest описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpellCastRequest {

    /** Active battle id; when set, the spell's action cost is spent on the caster's combat turn. */
    private UUID combatId;

    /**
     * Slot level to cast with (upcasting). Defaults to the spell's own level; must not be lower.
     * Ignored for cantrips (level 0), which never spend a slot.
     */
    private Integer slotLevel;

    /**
     * Character the spell's active effects land on (Bless-style buffs). Defaults to the caster.
     * Rolled damage/healing is applied separately via the {@code /apply} endpoint per target.
     */
    private UUID targetCharacterId;
}
