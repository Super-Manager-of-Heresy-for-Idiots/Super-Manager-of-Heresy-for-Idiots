package com.dnd.app.repository;

import com.dnd.app.domain.BuffDebuff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BuffDebuffRepository extends JpaRepository<BuffDebuff, UUID> {

    boolean existsByName(String name);

    List<BuffDebuff> findAllByIsBuff(Boolean isBuff);

    List<BuffDebuff> findAllByEffectType(String effectType);

    List<BuffDebuff> findAllByIsBuffAndEffectType(Boolean isBuff, String effectType);

    @Modifying
    @Query("update BuffDebuff bd set bd.deprecated = true where bd.targetStat.id = :statTypeId")
    int markDeprecatedByTargetStatId(@Param("statTypeId") UUID statTypeId);
}
