package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * One selectable option within a CHOICE reward group. Carries the grants applied
 * when the player picks it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "RewardOption", description = "A selectable option within a reward group")
public class RewardOptionDto {

    @Schema(description = "Option id")
    private UUID id;

    @Schema(description = "Stable business key of the option within its group", example = "subclass_champion")
    private String optionKey;

    @Schema(description = "Display label resolved for locale", example = "Path of Thunder")
    private String label;
    private String labelRu;
    private String labelEn;

    private String description;

    @Schema(description = "Whether this option is recommended", example = "true")
    private Boolean recommended;

    @Schema(description = "Ordering within the group", example = "0")
    private Integer sortOrder;

    @Schema(description = "Grants applied when this option is chosen")
    private List<RewardGrantDto> grants;
}
