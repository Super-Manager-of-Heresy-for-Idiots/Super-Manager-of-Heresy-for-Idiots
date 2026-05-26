package com.dnd.app.repository;

import com.dnd.app.domain.CharacterClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CharacterClassRepository extends JpaRepository<CharacterClass, UUID> {

    boolean existsByName(String name);
}
