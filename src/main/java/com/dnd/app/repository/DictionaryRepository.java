package com.dnd.app.repository;

import com.dnd.app.domain.DictionaryEntry;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@NoRepositoryBean
public interface DictionaryRepository<T extends DictionaryEntry> extends JpaRepository<T, UUID> {

    List<T> findAllByHomebrewIsNull();

    List<T> findAllByHomebrewId(UUID homebrewId);

    List<T> findAllByHomebrewIdIn(Set<UUID> homebrewIds);

    Optional<T> findByCodeAndHomebrewIsNull(String code);

    boolean existsByCodeAndHomebrewIsNull(String code);

    boolean existsByCodeAndHomebrewId(String code, UUID homebrewId);
}
