package com.dnd.app.repository;

import com.dnd.app.domain.BattleLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Контракт BattleLogRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface BattleLogRepository extends JpaRepository<BattleLog, UUID> {

    /** Highest seq recorded for a battle, or null when the log is empty (next seq = this + 1). */
    @Query("select max(l.seq) from BattleLog l where l.battleId = :battleId")
    Long findMaxSeq(@Param("battleId") UUID battleId);

    /** Entries after {@code afterSeq}, seq-ascending, page-limited — the GM sees everything. */
    List<BattleLog> findByBattleIdAndSeqGreaterThanOrderBySeqAsc(UUID battleId, long afterSeq, Pageable pageable);

    /** Same window, but excluding a visibility class — used to hide GM_ONLY entries from players. */
    List<BattleLog> findByBattleIdAndSeqGreaterThanAndVisibilityNotOrderBySeqAsc(
            UUID battleId, long afterSeq, com.dnd.app.domain.enums.BattleLogVisibility excluded, Pageable pageable);
}
