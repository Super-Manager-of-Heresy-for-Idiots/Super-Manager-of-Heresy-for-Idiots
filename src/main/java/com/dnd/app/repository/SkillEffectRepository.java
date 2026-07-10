package com.dnd.app.repository;

import com.dnd.app.domain.SkillEffect;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт SkillEffectRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface SkillEffectRepository extends JpaRepository<SkillEffect, UUID> {

    List<SkillEffect> findAllBySkillId(UUID skillId);

    void deleteAllBySkillId(UUID skillId);

    long countByBuffDebuffId(UUID buffDebuffId);
}
