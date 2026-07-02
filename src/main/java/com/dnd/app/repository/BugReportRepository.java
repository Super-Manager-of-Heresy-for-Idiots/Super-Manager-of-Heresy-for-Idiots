package com.dnd.app.repository;

import com.dnd.app.domain.BugReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BugReportRepository extends JpaRepository<BugReport, UUID> {
}
