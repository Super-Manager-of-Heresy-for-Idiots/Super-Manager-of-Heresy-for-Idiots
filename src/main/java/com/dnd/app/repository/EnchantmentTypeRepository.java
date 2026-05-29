package com.dnd.app.repository;

import com.dnd.app.domain.EnchantmentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EnchantmentTypeRepository extends JpaRepository<EnchantmentType, UUID> {

    boolean existsByName(String name);

    long countByBuffDebuffId(UUID buffDebuffId);
}
