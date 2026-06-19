package com.dnd.app.repository;

import com.dnd.app.domain.StatType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StatTypeRepository extends JpaRepository<StatType, UUID> {

    List<StatType> findByHomebrewIsNull();

    List<StatType> findByHomebrewIdIn(List<UUID> ids);

    boolean existsBySlugAndHomebrewIsNull(String slug);

    boolean existsBySlugAndHomebrew_Id(String slug, UUID homebrewId);
}
