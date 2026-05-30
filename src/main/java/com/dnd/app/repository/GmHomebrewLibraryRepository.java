package com.dnd.app.repository;

import com.dnd.app.domain.GmHomebrewLibrary;
import com.dnd.app.domain.GmHomebrewLibraryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GmHomebrewLibraryRepository extends JpaRepository<GmHomebrewLibrary, GmHomebrewLibraryId> {

    List<GmHomebrewLibrary> findByGmUserId(UUID gmUserId);

    boolean existsByGmUserIdAndPackageId(UUID gmUserId, UUID packageId);

    void deleteByGmUserIdAndPackageId(UUID gmUserId, UUID packageId);
}
