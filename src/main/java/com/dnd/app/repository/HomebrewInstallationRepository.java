package com.dnd.app.repository;

import com.dnd.app.domain.HomebrewInstallation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface HomebrewInstallationRepository extends JpaRepository<HomebrewInstallation, UUID> {

    boolean existsByHomebrewPackageIdAndInstallerId(UUID packageId, UUID installerId);

    Optional<HomebrewInstallation> findByIdAndInstallerId(UUID id, UUID installerId);

    Page<HomebrewInstallation> findAllByInstallerId(UUID installerId, Pageable pageable);

    long countByHomebrewPackageId(UUID packageId);

    @Query("SELECT i.homebrewPackage.id FROM HomebrewInstallation i WHERE i.installer.id = :installerId")
    Set<UUID> findPackageIdsByInstallerId(@Param("installerId") UUID installerId);
}
