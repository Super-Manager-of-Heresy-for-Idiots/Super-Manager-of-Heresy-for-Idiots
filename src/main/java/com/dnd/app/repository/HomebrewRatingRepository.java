package com.dnd.app.repository;

import com.dnd.app.domain.HomebrewRating;
import com.dnd.app.domain.HomebrewRatingId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт HomebrewRatingRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface HomebrewRatingRepository extends JpaRepository<HomebrewRating, HomebrewRatingId> {

    Optional<HomebrewRating> findByUserIdAndPackageId(UUID userId, UUID packageId);

    long countByPackageIdAndRating(UUID packageId, int rating);

    /**
     * Batch-агрегация рейтингов для набора пакетов (витрина, без N+1).
     * @param ids идентификаторы пакетов
     * @return строки-проекции {packageId, likes, dislikes}
     */
    @Query("SELECT r.packageId AS packageId, " +
            "SUM(CASE WHEN r.rating = 1 THEN 1 ELSE 0 END) AS likes, " +
            "SUM(CASE WHEN r.rating = -1 THEN 1 ELSE 0 END) AS dislikes " +
            "FROM HomebrewRating r WHERE r.packageId IN :ids GROUP BY r.packageId")
    List<RatingAggregate> aggregateByPackageIds(@Param("ids") Collection<UUID> ids);

    /** Проекция агрегата рейтинга пакета. */
    interface RatingAggregate {
        UUID getPackageId();
        long getLikes();
        long getDislikes();
    }
}
