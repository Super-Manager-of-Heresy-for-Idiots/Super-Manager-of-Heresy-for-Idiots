package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddContentRequest {

    @NotBlank(message = "Тип контента обязателен")
    private String contentType;

    @NotNull(message = "ID контента обязателен")
    private UUID contentId;
}
