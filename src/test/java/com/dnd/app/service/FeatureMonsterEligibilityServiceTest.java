package com.dnd.app.service;

import com.dnd.app.domain.featurerule.FeatureAllowedMonsterFilter;
import com.dnd.app.dto.featurerule.MonsterEligibilityResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for the pure monster-eligibility decision (creature type + max CR). */
class FeatureMonsterEligibilityServiceTest {

    private final FeatureMonsterEligibilityService service =
            new FeatureMonsterEligibilityService(null, null, null);

    private final UUID monsterId = UUID.randomUUID();

    private FeatureAllowedMonsterFilter filter(String creatureType) {
        return FeatureAllowedMonsterFilter.builder().creatureType(creatureType).build();
    }

    @Test
    void eligibleWhenTypeMatchesAndCrWithinMax() {
        MonsterEligibilityResult r =
                service.decide(monsterId, Set.of("beast"), BigDecimal.valueOf(2), filter("beast"), 4);
        assertThat(r.isEligible()).isTrue();
    }

    @Test
    void ineligibleWhenCrTooHigh() {
        MonsterEligibilityResult r =
                service.decide(monsterId, Set.of("beast"), BigDecimal.valueOf(6), filter("beast"), 4);
        assertThat(r.isEligible()).isFalse();
        assertThat(r.getReason()).contains("CR");
    }

    @Test
    void ineligibleWhenCreatureTypeMismatch() {
        MonsterEligibilityResult r =
                service.decide(monsterId, Set.of("beast"), BigDecimal.valueOf(1), filter("dragon"), null);
        assertThat(r.isEligible()).isFalse();
        assertThat(r.getReason()).contains("Тип существа");
    }

    @Test
    void eligibleWhenNoFilter() {
        MonsterEligibilityResult r =
                service.decide(monsterId, Set.of("beast"), BigDecimal.valueOf(20), null, null);
        assertThat(r.isEligible()).isTrue();
    }

    @Test
    void caseInsensitiveTypeMatch() {
        MonsterEligibilityResult r =
                service.decide(monsterId, Set.of("Beast"), BigDecimal.valueOf(1), filter("BEAST"), null);
        assertThat(r.isEligible()).isTrue();
    }
}
