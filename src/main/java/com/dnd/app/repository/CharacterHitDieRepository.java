package com.dnd.app.repository;

import com.dnd.app.domain.CharacterHitDie;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CharacterHitDieRepository extends JpaRepository<CharacterHitDie, UUID> {

    List<CharacterHitDie> findByCharacterId(UUID characterId);

    Optional<CharacterHitDie> findByCharacterIdAndDie(UUID characterId, Integer die);
}
