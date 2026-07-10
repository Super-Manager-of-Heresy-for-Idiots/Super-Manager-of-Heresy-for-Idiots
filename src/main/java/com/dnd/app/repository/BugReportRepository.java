package com.dnd.app.repository;

import com.dnd.app.domain.BugReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Контракт BugReportRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface BugReportRepository extends JpaRepository<BugReport, UUID> {
}
