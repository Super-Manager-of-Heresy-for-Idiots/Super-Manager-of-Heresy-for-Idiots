package com.dnd.app.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCharacterRequest {

    @NotBlank(message = "Character name is required")
    @Size(max = 100, message = "Character name must not exceed 100 characters")
    private String name;

    @Min(value = 1, message = "Level must be between 1 and 20")
    @Max(value = 20, message = "Level must be between 1 and 20")
    @Builder.Default
    private Integer level = 1;

    @NotNull(message = "Class ID is required")
    private UUID classId;

    @NotNull(message = "Race ID is required")
    private UUID raceId;
}
