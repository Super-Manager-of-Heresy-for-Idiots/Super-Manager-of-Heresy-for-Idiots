package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TargetTypeRepository extends JpaRepository<TargetType, UUID> {
}
