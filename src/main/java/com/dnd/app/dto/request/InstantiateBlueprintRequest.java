package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstantiateBlueprintRequest {

    @NotBlank(message = "Название кампании обязательно")
    @Size(max = 120, message = "Название не должно превышать 120 символов")
    private String name;

    private String description;
}
