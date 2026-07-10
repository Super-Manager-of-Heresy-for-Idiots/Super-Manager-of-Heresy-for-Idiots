package com.dnd.app.repository;

import com.dnd.app.domain.NpcShopItem;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
