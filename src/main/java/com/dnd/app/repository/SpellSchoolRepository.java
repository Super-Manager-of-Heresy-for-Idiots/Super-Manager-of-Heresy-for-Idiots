package com.dnd.app.repository;

import com.dnd.app.domain.SpellSchool;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpellSchoolRepository extends JpaRepository<SpellSchool, UUID> {

    List<SpellSchool> findAllByOrderByNameRuAsc();
}
