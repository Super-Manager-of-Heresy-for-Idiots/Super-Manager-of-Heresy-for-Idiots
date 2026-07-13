package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.ItemInstanceFeatureResource;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий ресурсов feature-rules, принадлежащих конкретным экземплярам предметов.
 */
public interface ItemInstanceFeatureResourceRepository extends JpaRepository<ItemInstanceFeatureResource, UUID> {
    List<ItemInstanceFeatureResource> findByItemInstanceId(UUID itemInstanceId);

    List<ItemInstanceFeatureResource> findByItemInstanceIdIn(Collection<UUID> itemInstanceIds);

    Optional<ItemInstanceFeatureResource> findByItemInstanceIdAndResourceDefinitionId(
            UUID itemInstanceId,
            UUID resourceDefinitionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from ItemInstanceFeatureResource r
            where r.itemInstanceId = :itemInstanceId and r.resourceDefinitionId = :resourceDefinitionId
            """)
    Optional<ItemInstanceFeatureResource> findByItemInstanceIdAndResourceDefinitionIdForUpdate(
            @Param("itemInstanceId") UUID itemInstanceId,
            @Param("resourceDefinitionId") UUID resourceDefinitionId);
}
