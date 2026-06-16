package com.dnd.app.repository;

import com.dnd.app.domain.content.CharacterRewardAbilityScoreSelection;
import com.dnd.app.domain.content.CharacterRewardAbilityScoreSelectionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CharacterRewardAbilityScoreSelectionRepository
        extends JpaRepository<CharacterRewardAbilityScoreSelection, CharacterRewardAbilityScoreSelectionId> {

    List<CharacterRewardAbilityScoreSelection> findAllBySelectionId(UUID selectionId);
}
