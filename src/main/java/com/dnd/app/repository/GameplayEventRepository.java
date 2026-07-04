package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.GameplayEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GameplayEventRepository extends JpaRepository<GameplayEvent, UUID> {
}
