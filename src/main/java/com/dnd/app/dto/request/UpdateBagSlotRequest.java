package com.dnd.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBagSlotRequest {

    @Min(value = 1, message = "Количество должно быть не меньше 1")
    private Integer quantity;

    @Size(max = 255, message = "Заметка не должна превышать 255 символов")
    private String notes;
}
