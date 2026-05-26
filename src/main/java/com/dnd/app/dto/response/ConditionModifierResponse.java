package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionModifierResponse {
    private UUID id;
    private UUID statTypeId;
    private String statTypeName;
    private Integer modifierValue;
}
