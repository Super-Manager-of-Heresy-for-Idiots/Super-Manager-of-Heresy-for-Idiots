package com.dnd.app.repository;

import com.dnd.app.domain.HomebrewTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HomebrewTagRepository extends JpaRepository<HomebrewTag, UUID> {

    Optional<HomebrewTag> findByName(String name);

    List<HomebrewTag> findByNameIn(List<String> names);

    @Query("SELECT t.id FROM HomebrewTag t JOIN HomebrewPackage p ON t MEMBER OF p.tags " +
            "WHERE p.id = :packageId")
    List<UUID> findTagIdsByPackageId(UUID packageId);

    @Query("SELECT t.name, COUNT(p) FROM HomebrewTag t LEFT JOIN t.id tid " +
            "GROUP BY t.id, t.name")
    List<Object[]> findAllWithUsageCountRaw();

    @Query(value = "SELECT t.id, t.name, COUNT(pt.package_id) as usage_count " +
            "FROM homebrew_tags t LEFT JOIN homebrew_package_tags pt ON t.id = pt.tag_id " +
            "GROUP BY t.id, t.name ORDER BY t.name",
            nativeQuery = true)
    List<Object[]> findAllWithUsageCount();

    boolean existsByName(String name);
}
