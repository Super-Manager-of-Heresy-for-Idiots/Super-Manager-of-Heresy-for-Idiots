package com.dnd.app.repository;

import com.dnd.app.domain.CustomResourceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CustomResourceTypeRepository extends JpaRepository<CustomResourceType, UUID> {

    List<CustomResourceType> findByHomebrewIsNull();
}
