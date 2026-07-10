package com.dnd.app.repository;

import com.dnd.app.domain.content.ClassLevelRewardGrantSubclass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт ClassLevelRewardGrantSubclassRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface ClassLevelRewardGrantSubclassRepository extends JpaRepository<ClassLevelRewardGrantSubclass, UUID> {

    List<ClassLevelRewardGrantSubclass> findAllBySubclassId(UUID subclassId);
}
