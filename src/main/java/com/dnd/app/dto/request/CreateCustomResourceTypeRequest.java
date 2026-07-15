package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO CreateCustomResourceTypeRequest — тело авторинга homebrew-ресурса (P2-3), того же механизма, что Ярость/Ки
 * (custom_resource_types). Максимум задаётся фиксированным значением {@code maxValue} ИЛИ DSL-формулой
 * {@code maxFormula}; восстановление на коротком/длинном отдыхе — none|full|formula (+ формула при formula).
 * Опциональная привязка к классу {@code classBoundId}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomResourceTypeRequest {

    @NotBlank(message = "Название ресурса обязательно")
    @Size(max = 100)
    private String name;

    private String description;

    /** Фиксированный максимум (если нет формулы). */
    private Integer maxValue;

    /** DSL-формула максимума (напр. class_level("monk")); имеет приоритет над maxValue. */
    private String maxFormula;

    /** Восстановление на коротком отдыхе: none | full | formula. */
    private String shortRestRecovery;

    /** Формула зарядов, восстанавливаемых на коротком отдыхе (при shortRestRecovery=formula). */
    private String shortRestFormula;

    /** Восстановление на длинном отдыхе: none | full | formula. */
    private String longRestRecovery;

    /** Формула зарядов, восстанавливаемых на длинном отдыхе (при longRestRecovery=formula). */
    private String longRestFormula;

    /** Опциональная привязка ресурса к классу. */
    private UUID classBoundId;
}
