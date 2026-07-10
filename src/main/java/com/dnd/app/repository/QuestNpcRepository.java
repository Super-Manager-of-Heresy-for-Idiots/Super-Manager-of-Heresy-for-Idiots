package com.dnd.app.repository;

import com.dnd.app.domain.QuestNpc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт QuestNpcRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface QuestNpcRepository extends JpaRepository<QuestNpc, UUID> {

    List<QuestNpc> findByQuestId(UUID questId);
}
