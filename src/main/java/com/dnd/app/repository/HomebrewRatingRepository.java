package com.dnd.app.repository;

import com.dnd.app.domain.HomebrewRating;
import com.dnd.app.domain.HomebrewRatingId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Контракт HomebrewRatingRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface HomebrewRatingRepository extends JpaRepository<HomebrewRating, HomebrewRatingId> {

    Optional<HomebrewRating> findByUserIdAndPackageId(UUID userId, UUID packageId);

    long countByPackageIdAndRating(UUID packageId, int rating);
}
