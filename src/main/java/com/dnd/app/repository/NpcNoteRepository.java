package com.dnd.app.repository;

import com.dnd.app.domain.NpcNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт NpcNoteRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface NpcNoteRepository extends JpaRepository<NpcNote, UUID> {

    List<NpcNote> findByNpcId(UUID npcId);
}
