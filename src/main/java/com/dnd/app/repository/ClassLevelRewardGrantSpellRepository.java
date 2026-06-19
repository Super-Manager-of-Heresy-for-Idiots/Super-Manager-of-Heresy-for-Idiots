package com.dnd.app.repository;

import com.dnd.app.domain.content.ClassLevelRewardGrantSpell;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassLevelRewardGrantSpellRepository extends JpaRepository<ClassLevelRewardGrantSpell, UUID> {

    List<ClassLevelRewardGrantSpell> findAllBySpellId(UUID spellId);
}
