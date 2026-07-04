package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureSpellFilter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FeatureSpellFilterRepository extends JpaRepository<FeatureSpellFilter, UUID> {
}
