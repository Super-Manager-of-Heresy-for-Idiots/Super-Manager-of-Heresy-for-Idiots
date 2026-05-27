package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterConditionResponse {
    private UUID id;
    private UUID conditionId;
    private String conditionName;
    private String conditionDescription;
    private List<ConditionModifierResponse> modifiers;
    private UUID appliedById;
    private Instant appliedAt;
    private Boolean active;
}
