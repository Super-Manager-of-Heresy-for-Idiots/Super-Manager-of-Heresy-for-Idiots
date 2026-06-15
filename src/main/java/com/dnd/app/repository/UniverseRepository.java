package com.dnd.app.repository;

import com.dnd.app.domain.Universe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UniverseRepository extends JpaRepository<Universe, UUID> {

    Optional<Universe> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Universe> findAllByOrderByNameAsc();
}
