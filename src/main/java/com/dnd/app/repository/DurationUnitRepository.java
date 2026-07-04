package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.DurationUnit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DurationUnitRepository extends JpaRepository<DurationUnit, UUID> {
    Optional<DurationUnit> findByCode(String code);
}
