package com.dnd.app.repository;

import com.dnd.app.domain.ItemTemplateBuff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт ItemTemplateBuffRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface ItemTemplateBuffRepository extends JpaRepository<ItemTemplateBuff, UUID> {

    List<ItemTemplateBuff> findByTemplateId(UUID templateId);
}
