package com.dnd.app.dto.content.grant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * SPELL grant — grants fixed spells or a filtered choice. Reference-only:
 * spells are never created inline here.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "SpellGrantPayload", description = "Grants/chooses spells (payload for grantType=SPELL)")
public class SpellGrantPayload implements GrantPayload {

    @Schema(description = "FIXED => grant specific spells; CHOICE => filtered pool", example = "CHOICE",
            allowableValues = {"FIXED", "CHOICE"})
    private String mode;

    // mode = FIXED
    @Schema(description = "Spell ids to grant (mode=FIXED)")
    private List<UUID> fixedSpellIds;

    // mode = CHOICE (pool filters)
    @Schema(description = "Exact spell level, or null for a range", example = "1")
    private Integer spellLevel;
    @Schema(description = "Inclusive min spell level for a range")
    private Integer minLevel;
    @Schema(description = "Inclusive max spell level for a range")
    private Integer maxLevel;
    @Schema(description = "Restrict to these schools (magic school ids)")
    private List<UUID> schoolIds;
    @Schema(description = "Explicit spell list id, if the model uses spell lists")
    private UUID spellListId;
    @Schema(description = "Class spell list id")
    private UUID classSpellListId;
    @Schema(description = "How many spells to choose", example = "2")
    private Integer chooseCount;
    @Schema(description = "Whether a chosen spell may be replaced on level-up")
    private Boolean allowReplaceOnLevelUp;
}
