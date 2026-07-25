package com.dnd.app.repository;

import com.dnd.app.domain.NpcDialogue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Контракт NpcDialogueRepository описывает репозиторий опциональных диалогов NPC
 * (WORLD_PLAN Этап 4). Один диалог на NPC.
 */
public interface NpcDialogueRepository extends JpaRepository<NpcDialogue, UUID> {

    Optional<NpcDialogue> findByNpcId(UUID npcId);
}
