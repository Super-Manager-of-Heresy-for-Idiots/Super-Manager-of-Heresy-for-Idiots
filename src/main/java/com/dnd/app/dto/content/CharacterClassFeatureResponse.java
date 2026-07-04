package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * A structured class feature the character actually has (Reckless Attack, Danger Sense, Wild Shape…), resolved
 * from {@code class_feature} for the character's classes/levels. This is what the folio "Features" tab should
 * show — the real class abilities — instead of a free-text prose blob. Content-derived, so it is available for
 * every class regardless of the feature-rules runtime flags.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CharacterClassFeature", description = "A structured class feature the character has")
public class CharacterClassFeatureResponse {
    private UUID id;
    private UUID classId;
    private String className;
    private Integer level;
    private String title;
    private String description;
    private String activationType;
}
