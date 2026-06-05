package com.dnd.app.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSpellRequest {

    @NotBlank(message = "Название заклинания обязательно")
    @Size(max = 120)
    private String name;

    @Min(0) @Max(9)
    private int level;

    @NotBlank(message = "Школа магии обязательна")
    @Size(max = 30)
    private String school;

    private String description;

    private List<UUID> availableToClassIds;
}
