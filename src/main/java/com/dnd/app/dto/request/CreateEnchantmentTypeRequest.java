package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class CreateEnchantmentTypeRequest {

    @NotBlank(message = "Название обязательно")
    @Size(max = 100, message = "Название не должно превышать 100 символов")
    private String name;

    private String description;

    @Pattern(regexp = "^(\\d+)?d(\\d+)$", message = "Формат должен быть NdM или dM")
    private String damageDice;

    private Integer damageBonus;

    private String damageType;

    private UUID buffDebuffId;
}
