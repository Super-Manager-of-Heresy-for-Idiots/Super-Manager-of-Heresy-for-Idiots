package com.dnd.app.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite identifier for {@link CharacterSpellSlotUsage}: (character, spell level). */
public class CharacterSpellSlotUsageId implements Serializable {

    private UUID characterId;
    private Integer spellLevel;

    public CharacterSpellSlotUsageId() {
    }

    public CharacterSpellSlotUsageId(UUID characterId, Integer spellLevel) {
        this.characterId = characterId;
        this.spellLevel = spellLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CharacterSpellSlotUsageId that)) {
            return false;
        }
        return Objects.equals(characterId, that.characterId) && Objects.equals(spellLevel, that.spellLevel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(characterId, spellLevel);
    }
}
