package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/** Request to use a feature (characterId + featureId come from the path). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureUseRequest {
    private UUID combatId;
    private List<UUID> targetIds;
    private List<UUID> selectedChoiceIds;
}
