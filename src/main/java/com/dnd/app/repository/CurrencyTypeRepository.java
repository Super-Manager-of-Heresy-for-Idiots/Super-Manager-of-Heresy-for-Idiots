package com.dnd.app.repository;

import com.dnd.app.domain.CurrencyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CurrencyTypeRepository extends JpaRepository<CurrencyType, UUID> {

    List<CurrencyType> findByHomebrewIsNull();

    List<CurrencyType> findByHomebrewIdIn(List<UUID> ids);

    Optional<CurrencyType> findBySlugAndHomebrewIsNull(String slug);
}
