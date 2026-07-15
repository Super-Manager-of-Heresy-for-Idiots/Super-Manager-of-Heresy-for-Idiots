package com.dnd.app.repository;

import com.dnd.app.domain.content.DiceFormula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Контракт DiceFormulaRepository — доступ к формулам костей (dice_formula).
 * Используется авторингом homebrew-оружия для создания формулы урона (IT-2).
 */
public interface DiceFormulaRepository extends JpaRepository<DiceFormula, UUID> {
}
