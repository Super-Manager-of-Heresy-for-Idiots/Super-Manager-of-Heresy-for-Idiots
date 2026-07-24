package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.CharacterFeatureResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт CharacterFeatureResourceRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CharacterFeatureResourceRepository extends JpaRepository<CharacterFeatureResource, UUID> {
    List<CharacterFeatureResource> findByCharacterId(UUID characterId);
    Optional<CharacterFeatureResource> findByCharacterIdAndResourceDefinitionId(UUID characterId, UUID resourceDefinitionId);
    Optional<CharacterFeatureResource> findFirstByCharacterIdAndSharedPoolKey(UUID characterId, String sharedPoolKey);

    /**
     * Атомарно списывает {@code amount} единиц ресурса умения одним условным UPDATE (N7). Блокировка
     * строки сериализует конкурентные траты, устраняя гонку read-modify-write (TOCTOU), которая
     * позволяла потратить один и тот же заряд несколько раз. Списание происходит, только если ресурс
     * позволяет уходить в минус ({@code allowNegative}) либо остатка хватает ({@code current_value >= amount}).
     * Контракт ошибок не меняется: 0 изменённых строк ⇒ вызывающий бросает прежний {@code BadRequestException}.
     * @param id идентификатор строки ресурса
     * @param characterId идентификатор персонажа-владельца (defense-in-depth к проверке владения)
     * @param amount списываемое количество
     * @param allowNegative разрешено ли уводить остаток в минус (легальный флаг определения ресурса)
     * @return число изменённых строк: 1 — списано, 0 — недостаточно ресурса
     */
    @Modifying
    @Query(value = """
            UPDATE character_feature_resource
               SET current_value = current_value - :amount, updated_at = now()
             WHERE id = :id AND character_id = :characterId
               AND (:allowNegative = true OR current_value >= :amount)
            """, nativeQuery = true)
    int spendAtomically(@Param("id") UUID id, @Param("characterId") UUID characterId,
                        @Param("amount") int amount, @Param("allowNegative") boolean allowNegative);
}
