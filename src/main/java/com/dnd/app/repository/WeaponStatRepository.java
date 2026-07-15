package com.dnd.app.repository;

import com.dnd.app.domain.content.WeaponStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Контракт WeaponStatRepository — доступ к боевому блоку оружия (weapon_stat), shared-PK с equipment_item.
 * Используется авторингом homebrew-снаряжения (IT-2).
 */
public interface WeaponStatRepository extends JpaRepository<WeaponStat, UUID> {
}
