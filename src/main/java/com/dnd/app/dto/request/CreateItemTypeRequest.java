package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateItemTypeRequest {

    @NotBlank(message = "Название обязательно")
    @Size(max = 50, message = "Название не должно превышать 50 символов")
    private String name;

    private String description;

    @NotNull(message = "Слот обязателен")
    private String slot;

    @Pattern(regexp = "^(\\d+)?d(\\d+)$", message = "Формат должен быть NdM или dM")
    private String damageDice;

    private Integer damageBonus;

    private String damageType;

    private UUID skillId;

    private String skillActivation;
}
