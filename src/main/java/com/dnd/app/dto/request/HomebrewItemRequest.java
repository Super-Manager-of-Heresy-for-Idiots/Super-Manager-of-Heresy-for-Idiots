package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO HomebrewItemRequest — тело авторинга единого homebrew-предмета (P1.5 / IT-2). Снаружи предмет — одна
 * сущность; {@code kind} дискриминирует таблицу-назначение (пока реализован MAGIC; EQUIPMENT — следующий шаг,
 * TEMPLATE запрещён для новых определений). Kind-специфичные поля магического предмета: rarity, attunement.
 */
@Data
public class HomebrewItemRequest {

    /** MAGIC | EQUIPMENT (реализован MAGIC). */
    private String kind;

    @NotBlank(message = "Название предмета обязательно")
    @Size(max = 500)
    private String name;

    private String nameEn;

    private String description;

    /** Код редкости (magic). */
    private String rarity;

    private Boolean attunementRequired;

    private String attunementRequirement;
}
