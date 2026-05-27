package com.dnd.app.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LevelThresholdServiceTest {

    private final LevelThresholdService service = new LevelThresholdService();

    @Test
    void isReadyToLevelUp_exactlyAtThreshold_returnsTrue() {
        assertTrue(service.isReadyToLevelUp(300, 1));
        assertTrue(service.isReadyToLevelUp(900, 2));
        assertTrue(service.isReadyToLevelUp(2700, 3));
    }

    @Test
    void isReadyToLevelUp_aboveThreshold_returnsTrue() {
        assertTrue(service.isReadyToLevelUp(500, 1));
        assertTrue(service.isReadyToLevelUp(100000, 5));
    }

    @Test
    void isReadyToLevelUp_belowThreshold_returnsFalse() {
        assertFalse(service.isReadyToLevelUp(0, 1));
        assertFalse(service.isReadyToLevelUp(299, 1));
        assertFalse(service.isReadyToLevelUp(899, 2));
    }

    @Test
    void isReadyToLevelUp_atLevel20_returnsFalse() {
        assertFalse(service.isReadyToLevelUp(999999, 20));
    }

    @Test
    void xpToNextLevel_belowThreshold_returnsRemaining() {
        assertEquals(300, service.xpToNextLevel(0, 1));
        assertEquals(100, service.xpToNextLevel(200, 1));
    }

    @Test
    void xpToNextLevel_atOrAboveThreshold_returnsZero() {
        assertEquals(0, service.xpToNextLevel(300, 1));
        assertEquals(0, service.xpToNextLevel(500, 1));
    }

    @Test
    void xpToNextLevel_atLevel20_returnsZero() {
        assertEquals(0, service.xpToNextLevel(999999, 20));
    }
}
