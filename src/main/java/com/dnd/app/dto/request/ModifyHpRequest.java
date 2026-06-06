package com.dnd.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifyHpRequest {

    @NotNull(message = "Amount is required")
    private Integer amount;

    @Min(value = 0, message = "tempHp must be >= 0")
    private Integer setTempHp;
}
