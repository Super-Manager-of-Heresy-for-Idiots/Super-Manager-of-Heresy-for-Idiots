package com.dnd.app.repository;

import com.dnd.app.domain.ProficiencySkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ProficiencySkillRepository extends JpaRepository<ProficiencySkill, UUID> {

    Optional<ProficiencySkill> findByName(String name);

    List<ProficiencySkill> findByNameIn(List<String> names);

    List<ProficiencySkill> findByIdIn(Set<UUID> ids);

    @Modifying
    @Query("update ProficiencySkill ps set ps.deprecated = true where ps.governingStat.id = :statTypeId")
    int markDeprecatedByGoverningStatId(@Param("statTypeId") UUID statTypeId);
}
