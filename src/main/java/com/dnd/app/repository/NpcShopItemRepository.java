package com.dnd.app.repository;

import com.dnd.app.domain.NpcShopItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт NpcShopItemRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface NpcShopItemRepository extends JpaRepository<NpcShopItem, UUID> {

    List<NpcShopItem> findByNpcId(UUID npcId);

    Optional<NpcShopItem> findByNpcIdAndItemTemplateId(UUID npcId, UUID itemTemplateId);

    /**
     * Как {@link #findByNpcIdAndItemTemplateId}, но с блокировкой строки (PESSIMISTIC_WRITE).
     * Нужен в сценариях купли-продажи, чтобы конкурентные транзакции не увели остаток в минус
     * при проверке-и-изменении quantity (check-then-act).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from NpcShopItem s where s.npc.id = :npcId and s.itemTemplate.id = :itemTemplateId")
    Optional<NpcShopItem> findByNpcIdAndItemTemplateIdForUpdate(@Param("npcId") UUID npcId,
                                                                @Param("itemTemplateId") UUID itemTemplateId);
}
