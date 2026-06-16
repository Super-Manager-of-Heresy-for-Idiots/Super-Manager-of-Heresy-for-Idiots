package com.dnd.app.repository;

import com.dnd.app.domain.content.ClassLevelRewardGrantSubclass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassLevelRewardGrantSubclassRepository extends JpaRepository<ClassLevelRewardGrantSubclass, UUID> {

    List<ClassLevelRewardGrantSubclass> findAllBySubclassId(UUID subclassId);
}
