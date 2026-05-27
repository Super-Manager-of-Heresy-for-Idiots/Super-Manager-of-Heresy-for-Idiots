package com.dnd.app.repository;

import com.dnd.app.domain.CharacterRace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CharacterRaceRepository extends JpaRepository<CharacterRace, UUID> {

    boolean existsByName(String name);
}
