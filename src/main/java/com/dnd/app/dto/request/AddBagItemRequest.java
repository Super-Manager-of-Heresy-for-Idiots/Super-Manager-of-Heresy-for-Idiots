package com.dnd.app.dto.request;

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
public class AddBagItemRequest {

    private UUID itemTypeId;

    private UUID artifactId;

    @Min(value = 1, message = "Количество должно быть не меньше 1")
    @Builder.Default
    private Integer quantity = 1;

    @Size(max = 255, message = "Заметка не должна превышать 255 символов")
    private String notes;
}
