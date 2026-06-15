package com.dnd.app.repository;

import com.dnd.app.domain.CampaignBlueprint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CampaignBlueprintRepository extends JpaRepository<CampaignBlueprint, UUID> {

    Optional<CampaignBlueprint> findByIdAndAuthorId(UUID id, UUID authorId);

    Page<CampaignBlueprint> findAllByAuthorIdAndDeletedAtIsNull(UUID authorId, Pageable pageable);

    @Query("SELECT b FROM CampaignBlueprint b WHERE b.id = :id AND b.status = 'PUBLISHED' AND b.deletedAt IS NULL")
    Optional<CampaignBlueprint> findPublishedById(@Param("id") UUID id);

    @Query("SELECT b FROM CampaignBlueprint b WHERE b.status = 'PUBLISHED' AND b.deletedAt IS NULL " +
            "AND (:search IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(b.loreDescription) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:universeSlug IS NULL OR b.universe.slug = :universeSlug)")
    Page<CampaignBlueprint> findMarketplace(@Param("search") String search,
                                            @Param("universeSlug") String universeSlug,
                                            Pageable pageable);
}
