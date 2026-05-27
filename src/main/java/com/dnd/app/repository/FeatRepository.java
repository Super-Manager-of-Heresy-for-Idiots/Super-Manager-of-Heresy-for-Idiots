package com.dnd.app.repository;

import com.dnd.app.domain.Feat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FeatRepository extends JpaRepository<Feat, UUID> {
    boolean existsByName(String name);
}
