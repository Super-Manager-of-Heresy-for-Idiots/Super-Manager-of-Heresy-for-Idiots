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
public class CreateCharacterClassRequest {

    @NotBlank(message = "Название обязательно")
    @Size(max = 50, message = "Название не должно превышать 50 символов")
    private String name;

    private String description;
}
