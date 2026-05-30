package com.dnd.app.repository;

import com.dnd.app.domain.TeamHomebrewActivation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TeamHomebrewActivationRepository extends JpaRepository<TeamHomebrewActivation, UUID> {

    List<TeamHomebrewActivation> findAllByTeamId(UUID teamId);

    Optional<TeamHomebrewActivation> findByTeamIdAndHomebrewPackageId(UUID teamId, UUID homebrewPackageId);

    boolean existsByTeamIdAndHomebrewPackageId(UUID teamId, UUID homebrewPackageId);

    @Query("SELECT tha.homebrewPackage.id FROM TeamHomebrewActivation tha WHERE tha.team.id = :teamId")
    Set<UUID> findPackageIdsByTeamId(@Param("teamId") UUID teamId);
}
