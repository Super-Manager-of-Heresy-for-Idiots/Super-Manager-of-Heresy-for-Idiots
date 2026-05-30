package com.dnd.app.repository;

import com.dnd.app.domain.HomebrewContentVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HomebrewContentVersionRepository extends JpaRepository<HomebrewContentVersion, UUID> {

    List<HomebrewContentVersion> findByHomebrewPackageIdAndVersion(UUID packageId, Integer version);

    List<HomebrewContentVersion> findByHomebrewPackageId(UUID packageId);
}
