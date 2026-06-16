package com.dnd.app.repository;

import com.dnd.app.domain.content.CharacterRewardSpellSelection;
import com.dnd.app.domain.content.CharacterRewardSpellSelectionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CharacterRewardSpellSelectionRepository
        extends JpaRepository<CharacterRewardSpellSelection, CharacterRewardSpellSelectionId> {

    List<CharacterRewardSpellSelection> findAllBySelectionId(UUID selectionId);
}
