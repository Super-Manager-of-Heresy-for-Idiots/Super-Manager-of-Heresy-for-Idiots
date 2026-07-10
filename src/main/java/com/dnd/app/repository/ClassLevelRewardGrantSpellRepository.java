package com.dnd.app.repository;

import com.dnd.app.domain.content.ClassLevelRewardGrantSpell;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт ClassLevelRewardGrantSpellRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface ClassLevelRewardGrantSpellRepository extends JpaRepository<ClassLevelRewardGrantSpell, UUID> {

    List<ClassLevelRewardGrantSpell> findAllBySpellId(UUID spellId);
}
