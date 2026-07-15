package com.dnd.app.repository;

import com.dnd.app.domain.content.ArmorStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Контракт ArmorStatRepository — доступ к блоку брони (armor_stat), shared-PK с equipment_item.
 * Используется авторингом homebrew-снаряжения (IT-2).
 */
public interface ArmorStatRepository extends JpaRepository<ArmorStat, UUID> {
}
