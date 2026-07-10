package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.RuleSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт RuleSourceRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface RuleSourceRepository extends JpaRepository<RuleSource, UUID> {
    Optional<RuleSource> findByKey(String key);
    List<RuleSource> findAllByOrderByDisplayNameAsc();
}
