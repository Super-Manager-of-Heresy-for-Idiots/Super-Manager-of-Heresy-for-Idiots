package com.dnd.app.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class UpdateCharacterRequest {

    @Size(max = 100, message = "Character name must not exceed 100 characters")
    private String name;

    @Min(value = 1, message = "Level must be between 1 and 20")
    @Max(value = 20, message = "Level must be between 1 and 20")
    private Integer level;

    private UUID classId;

    private UUID raceId;
}
