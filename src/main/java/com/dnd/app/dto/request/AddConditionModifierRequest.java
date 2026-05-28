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
public class AddConditionModifierRequest {

    @NotNull(message = "ID характеристики обязателен")
    private UUID statTypeId;

    @NotNull(message = "Значение модификатора обязательно")
    private Integer modifierValue;
}
