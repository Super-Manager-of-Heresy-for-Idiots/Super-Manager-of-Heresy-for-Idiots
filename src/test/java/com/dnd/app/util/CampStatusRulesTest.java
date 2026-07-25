package com.dnd.app.util;

import com.dnd.app.domain.enums.CampStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CampStatusRules: state-machine привала")
class CampStatusRulesTest {

    @Test
    @DisplayName("Разбивка лагеря ведёт только в ACTIVE или COMPLETED")
    void settingUpAllowsStartAndCompletion() {
        assertThat(CampStatusRules.allowedTransitions(CampStatus.SETTING_UP))
                .containsExactly(CampStatus.ACTIVE, CampStatus.COMPLETED);
    }

    @Test
    @DisplayName("Отдых объявляется только из стоящего лагеря")
    void restingIsReachableOnlyFromActive() {
        assertThat(CampStatusRules.canTransition(CampStatus.ACTIVE, CampStatus.RESTING)).isTrue();
        assertThat(CampStatusRules.canTransition(CampStatus.SETTING_UP, CampStatus.RESTING)).isFalse();
        assertThat(CampStatusRules.canTransition(CampStatus.INTERRUPTED, CampStatus.RESTING)).isFalse();
    }

    @Test
    @DisplayName("После прерывания отряд возвращается в лагерь или сворачивает привал")
    void interruptedReturnsToActiveOrCompletes() {
        assertThat(CampStatusRules.allowedTransitions(CampStatus.INTERRUPTED))
                .containsExactly(CampStatus.ACTIVE, CampStatus.COMPLETED);
    }

    @Test
    @DisplayName("Завершённый привал терминален — переходов нет")
    void completedIsTerminal() {
        assertThat(CampStatusRules.allowedTransitions(CampStatus.COMPLETED)).isEmpty();
        assertThat(CampStatusRules.canTransition(CampStatus.COMPLETED, CampStatus.ACTIVE)).isFalse();
    }
}
