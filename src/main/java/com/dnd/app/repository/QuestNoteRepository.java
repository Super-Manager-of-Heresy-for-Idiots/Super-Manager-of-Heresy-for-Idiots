package com.dnd.app.repository;

import com.dnd.app.domain.QuestNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт QuestNoteRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface QuestNoteRepository extends JpaRepository<QuestNote, UUID> {

    List<QuestNote> findByQuestId(UUID questId);
}
