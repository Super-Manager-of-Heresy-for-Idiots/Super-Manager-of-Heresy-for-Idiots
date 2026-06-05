package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateProficiencySkillRequest {

    @NotBlank(message = "Название навыка обязательно")
    @Size(max = 60)
    private String name;

    @NotNull(message = "ID управляющей характеристики обязателен")
    private UUID governingStatId;
}
