package com.dnd.app.repository;

import com.dnd.app.domain.CharacterClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface CharacterClassRepository extends JpaRepository<CharacterClass, UUID> {

    boolean existsByName(String name);

    List<CharacterClass> findAllByHomebrewIsNull();

    List<CharacterClass> findAllByHomebrewIdIn(Set<UUID> packageIds);

    @Modifying
    @Query("update CharacterClass cc set cc.deprecated = true " +
            "where cc.primaryAbilityStat.id = :statTypeId or cc.spellcastingStat.id = :statTypeId")
    int markDeprecatedByStatTypeId(@Param("statTypeId") UUID statTypeId);
}
