package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.RestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RestTypeRepository extends JpaRepository<RestType, UUID> {
    Optional<RestType> findByCode(String code);
}
