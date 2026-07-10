package com.dnd.app.repository;

import com.dnd.app.domain.QuestReward;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт QuestRewardRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface QuestRewardRepository extends JpaRepository<QuestReward, UUID> {

    List<QuestReward> findByQuestId(UUID questId);
}
