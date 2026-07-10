package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureChoiceOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Контракт FeatureChoiceOptionRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface FeatureChoiceOptionRepository extends JpaRepository<FeatureChoiceOption, UUID> {
    List<FeatureChoiceOption> findByChoiceGroupIdOrderBySortOrderAsc(UUID choiceGroupId);
    List<FeatureChoiceOption> findByChoiceGroupIdIn(Collection<UUID> choiceGroupIds);
    void deleteByChoiceGroupIdIn(Collection<UUID> choiceGroupIds);
}
