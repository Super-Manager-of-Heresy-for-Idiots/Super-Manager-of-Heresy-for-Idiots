package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Summary of a class feature. Base-class features have {@code subclassId == null};
 * subclass features carry the owning subclass id.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ClassFeatureSummary", description = "Summary of a class (or subclass) feature")
public class ClassFeatureSummaryDto {

    @Schema(description = "Feature id")
    private UUID id;

    @Schema(description = "Stable slug", example = "storm-sense")
    private String slug;

    @Schema(description = "Owning class id")
    private UUID classId;

    @Schema(description = "Owning subclass id (null for base class features)")
    private UUID subclassId;

    @Schema(description = "Class level the feature is gained", example = "1")
    private Integer level;

    @Schema(description = "Ordering among features at the same level", example = "0")
    private Integer sortOrder;

    @Schema(description = "Feature title", example = "Storm Sense")
    private String title;

    private String description;
}
