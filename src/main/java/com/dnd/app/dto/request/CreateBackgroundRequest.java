package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Класс CreateBackgroundRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBackgroundRequest {

    @NotBlank(message = "Название предыстории обязательно")
    @Size(max = 100)
    private String name;

    private String nameEn;

    private String description;

    /** Названия навыков-владений (резолвятся по name_ru таблицы skill). */
    private List<String> skillProficiencyNames;

    /** Свободный текст доп. владений/снаряжения (инструменты, языки и т. п.); дополняет описание. */
    private String grantedExtras;
}
