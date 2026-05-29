package com.dnd.app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetSkillEffectsRequest {

    @NotNull(message = "Список эффектов обязателен")
    @Valid
    private List<SkillEffectRequest> effects;
}
