package com.dnd.app.repository;

import com.dnd.app.domain.HomebrewRating;
import com.dnd.app.domain.HomebrewRatingId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface HomebrewRatingRepository extends JpaRepository<HomebrewRating, HomebrewRatingId> {

    Optional<HomebrewRating> findByUserIdAndPackageId(UUID userId, UUID packageId);

    long countByPackageIdAndRating(UUID packageId, int rating);
}
