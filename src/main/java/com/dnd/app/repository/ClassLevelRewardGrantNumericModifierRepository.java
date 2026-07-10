package com.dnd.app.repository;

import com.dnd.app.domain.content.ClassLevelRewardGrantNumericModifier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт ClassLevelRewardGrantNumericModifierRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface ClassLevelRewardGrantNumericModifierRepository
        extends JpaRepository<ClassLevelRewardGrantNumericModifier, UUID> {

    List<ClassLevelRewardGrantNumericModifier> findAllByModifierKey(String modifierKey);

    List<ClassLevelRewardGrantNumericModifier> findAllByTargetKind(String targetKind);
}
