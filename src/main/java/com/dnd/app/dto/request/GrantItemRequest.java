package com.dnd.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrantItemRequest {

    /** Id of the catalog item to grant (equipment_item, magic_item, or legacy item_template). */
    @NotNull(message = "Item ID is required")
    private UUID itemId;

    /** Which catalog table {@link #itemId} refers to: EQUIPMENT (default), MAGIC, or TEMPLATE (legacy). */
    private String itemKind;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Builder.Default
    private Integer quantity = 1;

    private String customName;

    private Boolean isUnique;
}
