package com.dnd.app.repository;

import com.dnd.app.domain.SharedStorage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SharedStorageRepository extends JpaRepository<SharedStorage, UUID> {

    List<SharedStorage> findByCampaignId(UUID campaignId);
}
