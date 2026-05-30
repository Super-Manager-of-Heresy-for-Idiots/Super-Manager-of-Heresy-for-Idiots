package com.dnd.app.repository;

import com.dnd.app.domain.ItemTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItemTemplateRepository extends JpaRepository<ItemTemplate, UUID> {

    List<ItemTemplate> findByHomebrewIsNull();

    List<ItemTemplate> findByHomebrewIdIn(List<UUID> homebrewIds);

    Page<ItemTemplate> findByHomebrewIsNullOrHomebrewIdIn(List<UUID> homebrewIds, Pageable pageable);
}
