package com.dnd.app.repository;

import com.dnd.app.domain.Subclass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubclassRepository extends JpaRepository<Subclass, UUID> {
    boolean existsByName(String name);
    List<Subclass> findAllByParentClassId(UUID classId);
}
