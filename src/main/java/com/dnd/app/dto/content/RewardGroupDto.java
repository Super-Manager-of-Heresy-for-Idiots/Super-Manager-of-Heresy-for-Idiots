package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * A reward group attached to a class at a given class level. AUTO groups grant
 * directly via {@code grants}; CHOICE groups present {@code options}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "RewardGroup", description = "A class-level reward group (AUTO or CHOICE)")
public class RewardGroupDto {

    @Schema(description = "Reward group id")
    private UUID id;

    @Schema(description = "Owning class id")
    private UUID classId;

    @Schema(description = "Optional class feature this group is tied to")
    private UUID classFeatureId;

    @Schema(description = "Class level at which this group applies", example = "3")
    private Integer classLevel;

    @Schema(description = "Group kind. Known: AUTO | CHOICE.", example = "CHOICE",
            allowableValues = {"AUTO", "CHOICE"})
    private String groupKind;

    @Schema(description = "Prompt shown for CHOICE groups", example = "Choose your storm path")
    private String prompt;

    private String description;

    @Schema(description = "Minimum selections (AUTO => 0)", example = "1")
    private Integer chooseMin;

    @Schema(description = "Maximum selections (AUTO => 0)", example = "1")
    private Integer chooseMax;

    @Schema(description = "Whether the group repeats on later levels", example = "false")
    private Boolean repeatable;

    @Schema(description = "Ordering among groups at the same level", example = "0")
    private Integer sortOrder;

    @Schema(description = "Selectable options (CHOICE groups)")
    private List<RewardOptionDto> options;

    @Schema(description = "Group-level grants (AUTO groups)")
    private List<RewardGrantDto> grants;
}
