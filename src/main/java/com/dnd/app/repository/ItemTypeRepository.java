package com.dnd.app.repository;

import com.dnd.app.domain.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ItemTypeRepository extends JpaRepository<ItemType, UUID> {

    boolean existsByName(String name);
}
