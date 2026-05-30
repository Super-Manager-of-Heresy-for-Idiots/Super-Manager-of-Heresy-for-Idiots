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
public class UpdateNpcRequest {

    @Size(max = 100, message = "NPC name must not exceed 100 characters")
    private String name;

    private String publicDescription;

    private String privateDescription;

    private Boolean isVisibleToPlayers;
}
