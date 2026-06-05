package com.dnd.app.dto.response;

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
public class SpellResponse {
    private UUID id;
    private String name;
    private Integer level;
    private String school;
    private String description;
    private List<UUID> availableToClassIds;
}
