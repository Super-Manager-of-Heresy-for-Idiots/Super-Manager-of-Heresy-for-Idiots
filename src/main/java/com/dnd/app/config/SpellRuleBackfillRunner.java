package com.dnd.app.config;

import com.dnd.app.dto.featurerule.SpellRuleBackfillResult;
import com.dnd.app.service.SpellAreaBackfillService;
import com.dnd.app.service.SpellRuleBackfillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Integrates the existing structured spell mechanics (migrations 056–062: spell damage / healing /
 * save / action cost / buffs) into SPELL-owned feature rules on startup, so the feature-rules engine
 * can actually plan and resolve a spell (e.g. Fireball → 8d6 fire + Dex save). Without this the
 * spell-stack data exists but no {@code owner_type=SPELL} rules are created, so {@code planForSpell}
 * returns an empty plan (no preview, no damage).
 *
 * <p>Runs only when the spell subsystem is active ({@code app.feature-rules.runtime-enabled} +
 * {@code spells-enabled}). The backfill is idempotent per (spell, rule type) — already-created rules
 * are skipped — so re-running on every boot is safe and a no-op after the first pass. It is the same
 * operation the admin endpoint {@code POST /api/admin/feature-rules/backfill-spells?apply=true}
 * performs; doing it automatically means the target functionality is populated without a manual step.
 * Failures are logged and swallowed so a backfill problem never blocks application startup.</p>
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class SpellRuleBackfillRunner implements ApplicationRunner {

    private final FeatureRulesProperties flags;
    private final SpellRuleBackfillService spellRuleBackfillService;
    private final SpellAreaBackfillService spellAreaBackfillService;

    @Override
    public void run(ApplicationArguments args) {
        if (!flags.spellsActive()) {
            return;
        }
        long start = System.currentTimeMillis();
        try {
            SpellRuleBackfillResult result = spellRuleBackfillService.backfill(true);
            log.info("Spell rule backfill on startup: spells={}, tookMs={} (idempotent — existing rules skipped)",
                    result.getSpellsTotal(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("Spell rule backfill on startup failed (spell casting may fall back to empty plans): {}",
                    e.getMessage(), e);
        }
        try {
            int areaUpdated = spellAreaBackfillService.backfill();
            log.info("Spell area/zone backfill on startup: {} spells updated (idempotent)", areaUpdated);
        } catch (Exception e) {
            log.error("Spell area/zone backfill on startup failed (AoE templates unavailable): {}",
                    e.getMessage(), e);
        }
    }
}
