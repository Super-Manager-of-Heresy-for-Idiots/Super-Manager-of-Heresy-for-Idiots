package com.dnd.app.repository;

import com.dnd.app.domain.BuffDebuff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BuffDebuffRepository extends JpaRepository<BuffDebuff, UUID> {

    boolean existsByName(String name);

    List<BuffDebuff> findAllByIsBuff(Boolean isBuff);

    List<BuffDebuff> findAllByEffectType(String effectType);

    List<BuffDebuff> findAllByIsBuffAndEffectType(Boolean isBuff, String effectType);
}
