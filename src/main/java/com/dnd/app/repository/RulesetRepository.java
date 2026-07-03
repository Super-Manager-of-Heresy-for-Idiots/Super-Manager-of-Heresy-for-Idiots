package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.Ruleset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RulesetRepository extends JpaRepository<Ruleset, UUID> {
    Optional<Ruleset> findByKey(String key);
    List<Ruleset> findAllByOrderByEditionAsc();
}
