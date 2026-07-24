package com.dnd.app.repository;

import com.dnd.app.domain.CharacterSpellSlotUsage;
import com.dnd.app.domain.CharacterSpellSlotUsageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт CharacterSpellSlotUsageRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CharacterSpellSlotUsageRepository
        extends JpaRepository<CharacterSpellSlotUsage, CharacterSpellSlotUsageId> {

    List<CharacterSpellSlotUsage> findAllByCharacterId(UUID characterId);

    Optional<CharacterSpellSlotUsage> findByCharacterIdAndSpellLevel(UUID characterId, Integer spellLevel);

    /**
     * Атомарно списывает одну ячейку заклинания указанного уровня. Одним оператором
     * {@code INSERT ... ON CONFLICT DO UPDATE ... WHERE expended_count < :max} инкрементирует счётчик
     * потраченных ячеек только если ещё остались свободные. Конфликт по первичному ключу
     * {@code (character_id, spell_level)} берёт блокировку строки, поэтому конкурентные запросы
     * сериализуются — это устраняет гонку read-modify-write (TOCTOU), которая позволяла потратить
     * одну и ту же ячейку несколько раз.
     * @param characterId идентификатор персонажа
     * @param spellLevel уровень ячейки (1..9)
     * @param max максимально доступное число ячеек этого уровня
     * @return число изменённых строк: 1 — ячейка списана, 0 — все ячейки уже потрачены
     */
    @Modifying
    @Query(value = """
            INSERT INTO character_spell_slot_usage (character_id, spell_level, expended_count)
            VALUES (:characterId, :spellLevel, 1)
            ON CONFLICT (character_id, spell_level)
            DO UPDATE SET expended_count = character_spell_slot_usage.expended_count + 1
            WHERE character_spell_slot_usage.expended_count < :max
            """, nativeQuery = true)
    int expendSlotAtomically(@Param("characterId") UUID characterId,
                             @Param("spellLevel") int spellLevel,
                             @Param("max") int max);
}
