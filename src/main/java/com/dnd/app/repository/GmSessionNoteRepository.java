package com.dnd.app.repository;

import com.dnd.app.domain.GmSessionNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GmSessionNoteRepository extends JpaRepository<GmSessionNote, UUID> {

    List<GmSessionNote> findByCampaignId(UUID campaignId);

    List<GmSessionNote> findByCampaignIdAndAuthorId(UUID campaignId, UUID authorId);
}
