package com.dnd.app.repository;

import com.dnd.app.domain.CharacterRace;
import com.dnd.app.domain.enums.RaceSourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface CharacterRaceRepository extends JpaRepository<CharacterRace, UUID> {

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    List<CharacterRace> findAllByHomebrewIsNull();

    List<CharacterRace> findAllByHomebrewIsNullAndActiveTrue();

    List<CharacterRace> findAllByHomebrewIdIn(Set<UUID> packageIds);

    List<CharacterRace> findAllByHomebrewIdInAndActiveTrue(Set<UUID> packageIds);

    List<CharacterRace> findAllBySourceType(RaceSourceType sourceType);

    @Query("""
            select r from CharacterRace r
            where r.active = true
              and (
                    r.homebrew is null
                    or r.homebrew.id in :packageIds
                  )
            """)
    List<CharacterRace> findAvailableActive(@Param("packageIds") Set<UUID> packageIds);

    @Query("""
            select r from CharacterRace r
            where r.active = true
              and r.homebrew is null
            """)
    List<CharacterRace> findAvailableActiveSystemOnly();
}
