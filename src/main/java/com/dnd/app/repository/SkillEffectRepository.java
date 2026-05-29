package com.dnd.app.repository;

import com.dnd.app.domain.SkillEffect;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SkillEffectRepository extends JpaRepository<SkillEffect, UUID> {

    List<SkillEffect> findAllBySkillId(UUID skillId);

    void deleteAllBySkillId(UUID skillId);

    long countByBuffDebuffId(UUID buffDebuffId);
}
