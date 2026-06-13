package com.dnd.app.repository;

import com.dnd.app.domain.StatType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StatTypeRepository extends JpaRepository<StatType, UUID> {

    boolean existsByName(String name);

    boolean existsByCodeAndHomebrewIsNull(String code);

    boolean existsByCodeAndHomebrewId(String code, UUID homebrewId);
}
