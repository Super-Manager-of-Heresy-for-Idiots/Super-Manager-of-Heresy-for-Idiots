package com.dnd.app.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LevelThresholdService: пороги опыта и готовность к повышению уровня")
class LevelThresholdServiceTest {

    private final LevelThresholdService service = new LevelThresholdService();

    @Test
    @DisplayName("Опыт ровно на пороге — готов к повышению")
    void isReadyToLevelUp_exactlyAtThreshold_returnsTrue() {
        assertTrue(service.isReadyToLevelUp(300, 1));
        assertTrue(service.isReadyToLevelUp(900, 2));
        assertTrue(service.isReadyToLevelUp(2700, 3));
    }

    @Test
    @DisplayName("Опыт выше порога — готов к повышению")
    void isReadyToLevelUp_aboveThreshold_returnsTrue() {
        assertTrue(service.isReadyToLevelUp(500, 1));
        assertTrue(service.isReadyToLevelUp(100000, 5));
    }

    @Test
    @DisplayName("Опыт ниже порога — не готов к повышению")
    void isReadyToLevelUp_belowThreshold_returnsFalse() {
        assertFalse(service.isReadyToLevelUp(0, 1));
        assertFalse(service.isReadyToLevelUp(299, 1));
        assertFalse(service.isReadyToLevelUp(899, 2));
    }

    @Test
    @DisplayName("На 20 уровне повышение невозможно")
    void isReadyToLevelUp_atLevel20_returnsFalse() {
        assertFalse(service.isReadyToLevelUp(999999, 20));
    }

    @Test
    @DisplayName("До следующего уровня возвращается остаток опыта")
    void xpToNextLevel_belowThreshold_returnsRemaining() {
        assertEquals(300, service.xpToNextLevel(0, 1));
        assertEquals(100, service.xpToNextLevel(200, 1));
    }

    @Test
    @DisplayName("На пороге или выше до следующего уровня — ноль")
    void xpToNextLevel_atOrAboveThreshold_returnsZero() {
        assertEquals(0, service.xpToNextLevel(300, 1));
        assertEquals(0, service.xpToNextLevel(500, 1));
    }

    @Test
    @DisplayName("На 20 уровне до следующего уровня — ноль")
    void xpToNextLevel_atLevel20_returnsZero() {
        assertEquals(0, service.xpToNextLevel(999999, 20));
    }
}
