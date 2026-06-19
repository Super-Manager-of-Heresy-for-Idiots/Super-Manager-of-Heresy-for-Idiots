package com.dnd.app.repository;

import com.dnd.app.domain.content.MagicItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface MagicItemRepository extends JpaRepository<MagicItem, UUID> {

    List<MagicItem> findAllByHomebrewIsNull();

    List<MagicItem> findAllByHomebrewIdIn(Set<UUID> packageIds);
}
