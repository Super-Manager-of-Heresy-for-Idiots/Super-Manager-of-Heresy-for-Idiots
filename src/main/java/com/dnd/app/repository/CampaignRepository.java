package com.dnd.app.repository;

import com.dnd.app.domain.Campaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт CampaignRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    Optional<Campaign> findByInviteCode(String inviteCode);

    List<Campaign> findByIdIn(List<UUID> ids);

    Page<Campaign> findByIdIn(List<UUID> ids, Pageable pageable);

    Page<Campaign> findAll(Pageable pageable);
}
