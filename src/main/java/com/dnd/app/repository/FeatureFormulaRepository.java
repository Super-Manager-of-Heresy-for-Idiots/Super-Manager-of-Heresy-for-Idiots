package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureFormula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FeatureFormulaRepository extends JpaRepository<FeatureFormula, UUID> {
}
