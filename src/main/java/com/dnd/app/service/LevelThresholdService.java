package com.dnd.app.service;

import org.springframework.stereotype.Service;

@Service
public class LevelThresholdService {

    private static final long[] XP_THRESHOLDS = {
            0,       // Level 1
            300,     // Level 2
            900,     // Level 3
            2700,    // Level 4
            6500,    // Level 5
            14000,   // Level 6
            23000,   // Level 7
            34000,   // Level 8
            48000,   // Level 9
            64000,   // Level 10
            85000,   // Level 11
            100000,  // Level 12
            120000,  // Level 13
            140000,  // Level 14
            165000,  // Level 15
            240000,  // Level 16
            265000,  // Level 17
            355000,  // Level 18
            405000,  // Level 19
            475000   // Level 20
    };

    public boolean isReadyToLevelUp(long experience, int currentTotalLevel) {
        if (currentTotalLevel >= 20) {
            return false;
        }
        return experience >= XP_THRESHOLDS[currentTotalLevel];
    }

    public long xpToNextLevel(long experience, int currentTotalLevel) {
        if (currentTotalLevel >= 20) {
            return 0;
        }
        long threshold = XP_THRESHOLDS[currentTotalLevel];
        return Math.max(0, threshold - experience);
    }
}
