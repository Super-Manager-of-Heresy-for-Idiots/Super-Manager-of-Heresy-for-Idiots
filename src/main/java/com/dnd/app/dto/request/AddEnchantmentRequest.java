package com.dnd.app.dto.request;

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
public class AddEnchantmentRequest {

    @NotNull(message = "enchantmentTypeId обязателен")
    private UUID enchantmentTypeId;

    @Size(max = 255, message = "Заметка не должна превышать 255 символов")
    private String notes;
}
