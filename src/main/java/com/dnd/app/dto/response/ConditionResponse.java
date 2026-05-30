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
public class ConditionResponse {
    private UUID id;
    private String name;
    private String description;
    private List<ConditionModifierResponse> modifiers;
    private UUID createdById;
    private UUID teamId;
    private Instant createdAt;
}
