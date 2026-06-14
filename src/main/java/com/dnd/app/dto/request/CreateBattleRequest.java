package com.dnd.app.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBattleRequest {

    @Size(max = 120, message = "Battle name must be at most 120 characters")
    private String name;
}
