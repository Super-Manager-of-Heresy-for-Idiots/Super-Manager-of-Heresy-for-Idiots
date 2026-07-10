package com.dnd.app.repository;

import com.dnd.app.domain.content.ClassLevelRewardGrantCustomText;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт ClassLevelRewardGrantCustomTextRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface ClassLevelRewardGrantCustomTextRepository extends JpaRepository<ClassLevelRewardGrantCustomText, UUID> {

    List<ClassLevelRewardGrantCustomText> findAllByUserEditable(Boolean userEditable);
}
