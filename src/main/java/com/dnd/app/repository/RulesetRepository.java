package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.Ruleset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт RulesetRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface RulesetRepository extends JpaRepository<Ruleset, UUID> {
    Optional<Ruleset> findByKey(String key);
    List<Ruleset> findAllByOrderByEditionAsc();
}
