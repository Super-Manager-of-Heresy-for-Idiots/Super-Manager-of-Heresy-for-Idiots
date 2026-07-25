package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс CreateCampActivityRequest описывает кастомную даунтайм-активность кампании.
 * Механических автоэффектов у активности нет — награды выдаёт мастер обычными инструментами.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCampActivityRequest {

    @NotBlank(message = "Activity name is required")
    @Size(max = 100, message = "Activity name must not exceed 100 characters")
    private String name;

    private String description;

    /** Код глифа для интерфейса. */
    @Size(max = 40, message = "Icon code must not exceed 40 characters")
    private String iconCode;

    /** Подсказка мастеру, какую проверку уместно запросить. */
    @Size(max = 60, message = "Skill check reference must not exceed 60 characters")
    private String skillCheckRef;
}
