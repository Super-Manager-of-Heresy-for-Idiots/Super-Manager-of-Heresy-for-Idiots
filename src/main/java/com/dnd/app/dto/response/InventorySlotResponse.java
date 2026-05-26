package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InventorySlotResponse {
    private UUID id;
    private String slot;
    private UUID itemTypeId;
    private String itemTypeName;
    private UUID artifactId;
    private String artifactName;
    private String artifactRarity;
    private Integer quantity;
    private String notes;
}
