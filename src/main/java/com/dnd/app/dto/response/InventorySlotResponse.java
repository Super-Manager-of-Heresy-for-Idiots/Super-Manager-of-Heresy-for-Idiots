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
public class InventorySlotResponse {
    private UUID id;
    private String slot;
    private UUID itemTypeId;
    private String itemTypeName;
    private Integer quantity;
    private String notes;
}
