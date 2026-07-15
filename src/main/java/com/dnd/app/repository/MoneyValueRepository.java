package com.dnd.app.repository;

import com.dnd.app.domain.content.MoneyValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Контракт MoneyValueRepository — доступ к денежным значениям (money_value).
 * Используется авторингом homebrew-снаряжения для задания стоимости (IT-2).
 */
public interface MoneyValueRepository extends JpaRepository<MoneyValue, UUID> {
}
