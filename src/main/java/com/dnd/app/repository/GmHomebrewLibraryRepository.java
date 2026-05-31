package com.dnd.app.repository;

import com.dnd.app.domain.GmHomebrewLibrary;
import com.dnd.app.domain.GmHomebrewLibraryId;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface GmHomebrewLibraryRepository extends JpaRepository<GmHomebrewLibrary, GmHomebrewLibraryId> {

    List<GmHomebrewLibrary> findByGmUserId(UUID gmUserId);

    boolean existsByGmUserIdAndPackageId(UUID gmUserId, UUID packageId);

    void deleteByGmUserIdAndPackageId(UUID gmUserId, UUID packageId);

    long countByPackageId(UUID packageId);

    void deleteByPackageId(UUID packageId);

    @Query("SELECT g.packageId FROM GmHomebrewLibrary g WHERE g.gmUserId = :gmUserId")
    Set<UUID> findPackageIdsByGmUserId(@Param("gmUserId") UUID gmUserId);
}
