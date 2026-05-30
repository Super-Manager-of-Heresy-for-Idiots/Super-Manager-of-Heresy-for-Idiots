package com.dnd.app.repository;

import com.dnd.app.domain.NpcNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NpcNoteRepository extends JpaRepository<NpcNote, UUID> {

    List<NpcNote> findByNpcId(UUID npcId);
}
