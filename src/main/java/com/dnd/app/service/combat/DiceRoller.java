package com.dnd.app.service.combat;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Thin, injectable wrapper over RNG so combat rolls can be stubbed in tests.
 */
@Component
public class DiceRoller {

    /** Uniformly rolls a d20 (1–20 inclusive). */
    public int rollD20() {
        return ThreadLocalRandom.current().nextInt(1, 21);
    }
}
