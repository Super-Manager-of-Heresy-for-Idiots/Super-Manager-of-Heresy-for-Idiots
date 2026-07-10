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
 * Класс SpellRuleBackfillRunner описывает конфигурационный компонент, который подключает инфраструктуру к бизнес-сценариям приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class SpellRuleBackfillRunner implements ApplicationRunner {

    private final FeatureRulesProperties flags;
    private final SpellRuleBackfillService spellRuleBackfillService;
    private final SpellAreaBackfillService spellAreaBackfillService;

    /**
     * Выполняет операции "run" в рамках бизнес-логики инфраструктуры.
     * @param args входящее значение args, используемое бизнес-сценарием
     */
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
