package com.dnd.app.dto.content.grant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * FEAT grant — grants a fixed feat or lets the player choose any feat.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "FeatGrantPayload", description = "Grants/chooses a feat (payload for grantType=FEAT)")
public class FeatGrantPayload implements GrantPayload {

    @Schema(description = "FIXED => grant a specific feat; ANY => player chooses", example = "ANY",
            allowableValues = {"FIXED", "ANY"})
    private String mode;

    // mode = FIXED
    @Schema(description = "Existing feat id (mode=FIXED)")
    private UUID featId;

    @Schema(description = "Inline homebrew feat (mode=FIXED) — materialized into feat with homebrew ownership")
    private InlineFeat inlineFeat;

    // mode = ANY
    @Schema(description = "How many feats to choose (mode=ANY, default 1)", example = "1")
    private Integer chooseCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "InlineFeat", description = "Inline homebrew feat definition")
    public static class InlineFeat {
        @Schema(example = "Storm Caller")
        private String name;
        private String prerequisiteText;
        private String description;
    }
}
