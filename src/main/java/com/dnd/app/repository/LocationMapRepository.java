package com.dnd.app.repository;

import com.dnd.app.domain.LocationMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт LocationMapRepository описывает репозиторий привязок карт map-service
 * к локациям кампании (WORLD_PLAN Этап 4).
 */
public interface LocationMapRepository extends JpaRepository<LocationMap, UUID> {

    List<LocationMap> findByLocationId(UUID locationId);

    Optional<LocationMap> findByLocationIdAndExternalMapId(UUID locationId, UUID externalMapId);

    List<LocationMap> findByLocationIdAndIsDefaultTrue(UUID locationId);
}
