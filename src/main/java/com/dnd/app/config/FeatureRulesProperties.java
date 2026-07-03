package com.dnd.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Feature flags for the class-feature rules runtime, bound from {@code app.feature-rules.*}.
 *
 * <p>{@link #runtimeEnabled} is the master switch. When it is {@code false} (the default) the
 * application behaves exactly as before: class-feature descriptions are shown as text and no
 * structured rule is executed. Each per-subsystem flag only takes effect when the master switch is
 * also enabled — use the {@code *Active()} helpers instead of the raw getters when gating runtime
 * behavior, so a single toggle can disable everything.</p>
 *
 * <p>Introduced in Stage 0; all flags default to {@code false} so no behavior changes until later
 * stages explicitly enable a subsystem.</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.feature-rules")
public class FeatureRulesProperties {

    /** Master switch for the entire feature-rules runtime. */
    private boolean runtimeEnabled = false;

    /** Feature resources, charges and rest recovery (Stage 5). */
    private boolean resourcesEnabled = false;

    /** Feature action costs and feature use / action economy (Stage 6). */
    private boolean actionsEnabled = false;

    /** Active effects, conditions, modifiers and expiration (Stage 7). */
    private boolean effectsEnabled = false;

    /** Triggers, reactions and durable prompts (Stage 11). */
    private boolean triggersEnabled = false;

    /** Forms, transformations and companions (Stage 10). */
    private boolean formsEnabled = false;

    /** Spell / feature integration (Stage 9). */
    private boolean spellsEnabled = false;

    /** Resources subsystem is active only when the runtime master switch is also on. */
    public boolean resourcesActive() {
        return runtimeEnabled && resourcesEnabled;
    }

    /** Action economy subsystem is active only when the runtime master switch is also on. */
    public boolean actionsActive() {
        return runtimeEnabled && actionsEnabled;
    }

    /** Active-effects subsystem is active only when the runtime master switch is also on. */
    public boolean effectsActive() {
        return runtimeEnabled && effectsEnabled;
    }

    /** Triggers/reactions subsystem is active only when the runtime master switch is also on. */
    public boolean triggersActive() {
        return runtimeEnabled && triggersEnabled;
    }

    /** Forms/companions subsystem is active only when the runtime master switch is also on. */
    public boolean formsActive() {
        return runtimeEnabled && formsEnabled;
    }

    /** Spell-integration subsystem is active only when the runtime master switch is also on. */
    public boolean spellsActive() {
        return runtimeEnabled && spellsEnabled;
    }
}
