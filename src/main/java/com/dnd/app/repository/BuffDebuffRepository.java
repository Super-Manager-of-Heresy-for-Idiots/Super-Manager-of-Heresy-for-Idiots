package com.dnd.app.repository;

import com.dnd.app.domain.BuffDebuff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт BuffDebuffRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface BuffDebuffRepository extends JpaRepository<BuffDebuff, UUID> {

    boolean existsByName(String name);

    List<BuffDebuff> findAllByIsBuff(Boolean isBuff);

    List<BuffDebuff> findAllByEffectType(String effectType);

    List<BuffDebuff> findAllByIsBuffAndEffectType(Boolean isBuff, String effectType);

    // --- Origin-scoped finders (SEC-2 / P0-2) ---
    // Общие пикеры (редактор спеллов, зачарований и т.п.) должны видеть только ванильные бафы,
    // иначе приватные homebrew-бафы всех авторов утекают в общий список. homebrew_id IS NULL = ваниль.

    List<BuffDebuff> findAllByHomebrewIsNull();

    List<BuffDebuff> findAllByIsBuffAndHomebrewIsNull(Boolean isBuff);

    List<BuffDebuff> findAllByEffectTypeAndHomebrewIsNull(String effectType);

    List<BuffDebuff> findAllByIsBuffAndEffectTypeAndHomebrewIsNull(Boolean isBuff, String effectType);
}
