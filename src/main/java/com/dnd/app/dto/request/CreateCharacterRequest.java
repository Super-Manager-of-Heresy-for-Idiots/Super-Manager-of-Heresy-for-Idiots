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
public class CreateCharacterRequest {

    @NotBlank(message = "Character name is required")
    @Size(max = 100, message = "Character name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Class ID is required")
    private UUID classId;

    @NotNull(message = "Race ID is required")
    private UUID raceId;
}
