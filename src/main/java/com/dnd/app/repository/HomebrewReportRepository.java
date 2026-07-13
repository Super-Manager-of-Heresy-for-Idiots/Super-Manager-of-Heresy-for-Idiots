package com.dnd.app.repository;

import com.dnd.app.domain.HomebrewReport;
import com.dnd.app.domain.enums.HomebrewReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт HomebrewReportRepository описывает репозиторий жалоб на homebrew-пакеты (P2-6).
 */
public interface HomebrewReportRepository extends JpaRepository<HomebrewReport, UUID> {

    Page<HomebrewReport> findAllByStatusOrderByCreatedAtAsc(HomebrewReportStatus status, Pageable pageable);

    Page<HomebrewReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<HomebrewReport> findAllByHomebrewPackageIdAndStatus(UUID packageId, HomebrewReportStatus status);

    long countByStatus(HomebrewReportStatus status);
}
