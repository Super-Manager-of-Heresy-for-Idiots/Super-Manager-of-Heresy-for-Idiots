package com.dnd.app.dto.request;

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
public class ModifyResourceRequest {

    @NotNull(message = "Resource type ID is required")
    private UUID resourceTypeId;

    @NotNull(message = "Current value is required")
    private Integer currentValue;
}
