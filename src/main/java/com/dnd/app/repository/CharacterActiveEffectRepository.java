package com.dnd.app.repository;

import com.dnd.app.domain.CharacterActiveEffect;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CharacterActiveEffectRepository extends JpaRepository<CharacterActiveEffect, UUID> {

    List<CharacterActiveEffect> findByCharacterId(UUID characterId);

    void deleteByCharacterIdAndBuffDebuffId(UUID characterId, UUID buffDebuffId);
}
