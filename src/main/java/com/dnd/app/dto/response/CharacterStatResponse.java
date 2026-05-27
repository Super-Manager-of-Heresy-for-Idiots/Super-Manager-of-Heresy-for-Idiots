package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CharacterStatResponse {
    private UUID id;
    private UUID statTypeId;
    private String statTypeName;
    private Integer value;
    private Integer effectiveValue;
    private List<StatModifierDetail> activeModifiers;
}
