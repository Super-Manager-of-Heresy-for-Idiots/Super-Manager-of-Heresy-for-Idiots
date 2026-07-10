package com.dnd.app.repository;

import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.enums.HomebrewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт HomebrewPackageRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface HomebrewPackageRepository extends JpaRepository<HomebrewPackage, UUID> {

    Page<HomebrewPackage> findAllByAuthorId(UUID authorId, Pageable pageable);

    Page<HomebrewPackage> findAllByAuthorIdAndStatus(UUID authorId, HomebrewStatus status, Pageable pageable);

    Page<HomebrewPackage> findAllByAuthorIdAndDeletedAtIsNotNull(UUID authorId, Pageable pageable);

    @Query("SELECT p FROM HomebrewPackage p WHERE p.status = 'PUBLISHED' AND p.deletedAt IS NULL")
    Page<HomebrewPackage> findPublishedAndNotDeleted(Pageable pageable);

    @Query("SELECT p FROM HomebrewPackage p WHERE p.status = 'PUBLISHED' AND p.deletedAt IS NULL " +
            "AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<HomebrewPackage> findPublishedBySearch(@Param("search") String search, Pageable pageable);

    @Query("SELECT DISTINCT p FROM HomebrewPackage p JOIN p.tags t " +
            "WHERE p.status = 'PUBLISHED' AND p.deletedAt IS NULL " +
            "AND t.name IN :tagNames " +
            "GROUP BY p.id HAVING COUNT(DISTINCT t.name) = :tagCount")
    Page<HomebrewPackage> findPublishedByAllTags(@Param("tagNames") List<String> tagNames,
                                                  @Param("tagCount") long tagCount,
                                                  Pageable pageable);

    @Query("SELECT DISTINCT p FROM HomebrewPackage p JOIN p.tags t " +
            "WHERE p.status = 'PUBLISHED' AND p.deletedAt IS NULL " +
            "AND t.name IN :tagNames " +
            "AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "GROUP BY p.id HAVING COUNT(DISTINCT t.name) = :tagCount")
    Page<HomebrewPackage> findPublishedByAllTagsAndSearch(@Param("tagNames") List<String> tagNames,
                                                          @Param("tagCount") long tagCount,
                                                          @Param("search") String search,
                                                          Pageable pageable);

    Optional<HomebrewPackage> findByIdAndAuthorId(UUID id, UUID authorId);

    @Query("SELECT p FROM HomebrewPackage p WHERE p.id = :id AND p.status = 'PUBLISHED' AND p.deletedAt IS NULL")
    Optional<HomebrewPackage> findPublishedById(@Param("id") UUID id);

    Page<HomebrewPackage> findAllByStatus(HomebrewStatus status, Pageable pageable);

    Page<HomebrewPackage> findAllByAuthorIdIn(List<UUID> authorIds, Pageable pageable);
}
