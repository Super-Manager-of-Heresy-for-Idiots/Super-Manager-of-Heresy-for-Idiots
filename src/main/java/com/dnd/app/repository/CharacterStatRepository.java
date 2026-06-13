package com.dnd.app.repository;

import com.dnd.app.domain.CharacterStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CharacterStatRepository extends JpaRepository<CharacterStat, UUID> {

    List<CharacterStat> findAllByCharacterId(UUID characterId);

    @Modifying
    @Query("update CharacterStat cs set cs.deprecated = true where cs.statType.id = :statTypeId")
    int markDeprecatedByStatTypeId(@Param("statTypeId") UUID statTypeId);
}
