package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outcome of the spell → feature-rules backfill (S2 absorption), per source aspect. The reconciliation
 * invariant of every pass is {@code sourceSpells == rulesCreated + skippedExisting}: each spell whose
 * legacy columns carry usable data of that kind ends up with exactly one MIGRATION-sourced rule of the
 * matching type (created now, or already present from an earlier run).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpellRuleBackfillResult {

    private boolean applied;
    private int spellsTotal;

    private PassStats damage;
    private PassStats healing;
    private PassStats resolution;
    private PassStats actionCost;
    private PassStats effects;

    private int formulasCreated;
    private int issuesCreated;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PassStats {
        /** Spells with usable source data of this kind (the reconciliation base). */
        private int sourceSpells;
        /** Source rows seen (damage entries, healing entries, linked buffs, …). */
        private int sourceRows;
        private int rulesCreated;
        private int detailRowsCreated;
        /** Spells skipped because a MIGRATION rule of this type already exists (idempotency). */
        private int skippedExisting;
        /** Source rows without usable structured data (e.g. a damage entry with no dice). */
        private int skippedNoData;
    }
}
