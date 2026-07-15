package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Класс ItemRuleBackfillResult описывает DTO результата бэкфилла правил магических предметов.
 * Переносит счётчики прогона и заметки по каждому предмету между сервисом и API-слоем.
 * Зеркалит по духу {@link SpellRuleBackfillResult}, но с семантикой предметов
 * (создано / пропущено-существующих / ручных / пересечений).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemRuleBackfillResult {

    /** Были ли изменения реально применены (apply=true) или это dry-run. */
    private boolean applied;

    /** Всего обработано ванильных магических предметов. */
    private int itemsTotal;

    /** Создано правил (все статусы needs_review). */
    private int created;

    /** Пропущено из-за уже существующего MIGRATION-правила того же типа (идемпотентность). */
    private int skippedExisting;

    /** Создано issue ручной адъюдикации (manual_adjudication / непарсибельные фрагменты). */
    private int manual;

    /** Создано issue пересечения с легаси-баффами (LEGACY_BUFF_OVERLAP). */
    private int overlaps;

    /** Создано ресурсных определений зарядов (scope=ITEM_INSTANCE). */
    private int resourcesCreated;

    /** Создано грантов заклинаний (spell_grant). */
    private int spellGrantsCreated;

    /** Всего создано issue (любых типов). */
    private int issuesCreated;

    /** Построчные заметки по предметам (диагностика прогона). */
    private List<String> notes;
}
