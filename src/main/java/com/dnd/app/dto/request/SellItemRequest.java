package com.dnd.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** A character sells one or more units of a carried item to a merchant NPC. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellItemRequest {

    @NotNull(message = "Character ID is required")
    private UUID characterId;

    @NotNull(message = "Item instance ID is required")
    private UUID itemInstanceId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
