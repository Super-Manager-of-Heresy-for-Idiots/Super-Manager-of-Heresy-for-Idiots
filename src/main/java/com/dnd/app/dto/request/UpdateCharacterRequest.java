package com.dnd.app.dto.request;

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

    @Size(max = 100, message = "Имя персонажа не должно превышать 100 символов")
    private String name;

    private UUID raceId;
}
