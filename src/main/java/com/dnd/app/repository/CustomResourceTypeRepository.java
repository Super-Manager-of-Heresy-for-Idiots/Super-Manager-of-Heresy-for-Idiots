package com.dnd.app.repository;

import com.dnd.app.domain.CustomResourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Контракт CustomResourceTypeRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CustomResourceTypeRepository extends JpaRepository<CustomResourceType, UUID> {

    List<CustomResourceType> findByHomebrewIsNull();

    /** Resources bound to any of the given classes (custom_resource_types.class_bound_id). */
    List<CustomResourceType> findByClassBound_IdIn(Collection<UUID> classIds);

    /** Resources bound to any of the given feats (custom_resource_types.feat_bound_id). */
    List<CustomResourceType> findByFeatBound_IdIn(Collection<UUID> featIds);
}
