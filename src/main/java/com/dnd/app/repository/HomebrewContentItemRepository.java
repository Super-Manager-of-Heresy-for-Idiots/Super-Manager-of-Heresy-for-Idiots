package com.dnd.app.repository;

import com.dnd.app.domain.HomebrewContentItem;
import com.dnd.app.domain.enums.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface HomebrewContentItemRepository extends JpaRepository<HomebrewContentItem, UUID> {

    List<HomebrewContentItem> findAllByHomebrewPackageId(UUID packageId);

    boolean existsByHomebrewPackageIdAndContentTypeAndContentId(UUID packageId, ContentType contentType, UUID contentId);

    long countByHomebrewPackageId(UUID packageId);

    @Query("SELECT ci.contentType, COUNT(ci) FROM HomebrewContentItem ci " +
            "WHERE ci.homebrewPackage.id = :packageId GROUP BY ci.contentType")
    List<Object[]> countByPackageGroupedByType(@Param("packageId") UUID packageId);

    @Query("SELECT ci.contentId FROM HomebrewContentItem ci " +
            "WHERE ci.homebrewPackage.id IN :packageIds AND ci.contentType = :contentType")
    Set<UUID> findContentIdsByPackageIdsAndType(@Param("packageIds") Set<UUID> packageIds,
                                                 @Param("contentType") ContentType contentType);

    @Query("SELECT ci.contentId FROM HomebrewContentItem ci " +
            "WHERE ci.homebrewPackage.id IN :packageIds")
    Set<UUID> findContentIdsByPackageIds(@Param("packageIds") Set<UUID> packageIds);
}
