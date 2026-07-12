package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Подготовка действия (Ready, фаза 3.7): комбатант тратит своё действие, чтобы отложить его до
 * наступления триггера (например «когда враг войдёт в дверь — атакую»). Описание триггера/действия —
 * произвольный текст; при срабатывании тратится реакция (см. triggerReady). DoD: срабатывает по триггеру.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadyActionRequest {

    /** Текст подготовленного действия и его триггера. */
    @NotBlank(message = "Readied action description is required")
    @Size(max = 500, message = "Readied action description is too long")
    private String description;
}
