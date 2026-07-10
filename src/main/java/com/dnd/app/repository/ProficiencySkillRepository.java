package com.dnd.app.repository;

import com.dnd.app.domain.ProficiencySkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Контракт ProficiencySkillRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface ProficiencySkillRepository extends JpaRepository<ProficiencySkill, UUID> {

    Optional<ProficiencySkill> findByName(String name);

    List<ProficiencySkill> findByNameIn(List<String> names);

    List<ProficiencySkill> findByIdIn(Set<UUID> ids);
}
