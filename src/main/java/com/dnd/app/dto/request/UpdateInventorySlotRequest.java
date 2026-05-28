package com.dnd.app.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInventorySlotRequest {

    private UUID itemTypeId;

    @Min(value = 1, message = "Количество должно быть не меньше 1")
    @Builder.Default
    private Integer quantity = 1;

    private String notes;
}
