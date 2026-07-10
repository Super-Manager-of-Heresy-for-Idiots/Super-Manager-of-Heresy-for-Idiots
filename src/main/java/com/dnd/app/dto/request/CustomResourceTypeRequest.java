package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс CustomResourceTypeRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomResourceTypeRequest {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 4000)
    private String description;

    /** Fixed max (used when there is no formula). */
    private Integer maxValue;

    /** Bounded-DSL formula for the max, evaluated per character (e.g. {@code class_level("monk")}); blank = fixed max. */
    @Size(max = 2000)
    private String maxFormula;

    /** Class this resource belongs to (its members auto-provision it); null = unbound. */
    private UUID classBoundId;

    /** Feat this resource is granted by (e.g. Lucky → Luck Points); null = not feat-granted. */
    private UUID featBoundId;

    /** Rest that refills this resource: {@code none} | {@code short_rest} | {@code long_rest}. Deprecated — use the per-window fields. */
    @Size(max = 16)
    private String resetOn;

    /** Short-rest recovery mode: {@code none} | {@code full} | {@code formula}. */
    @Size(max = 16)
    private String shortRestRecovery;

    /** Charges restored on a short rest (DSL formula) when {@code shortRestRecovery=formula}. */
    @Size(max = 2000)
    private String shortRestFormula;

    /** Long-rest recovery mode: {@code none} | {@code full} | {@code formula}. */
    @Size(max = 16)
    private String longRestRecovery;

    /** Charges restored on a long rest (DSL formula) when {@code longRestRecovery=formula}. */
    @Size(max = 2000)
    private String longRestFormula;
}
