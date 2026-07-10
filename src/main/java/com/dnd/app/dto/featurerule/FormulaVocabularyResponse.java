package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Класс FormulaVocabularyResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormulaVocabularyResponse {
    private List<FormulaVocabEntry> functions;
    private List<FormulaVocabEntry> scalars;
    /** Ability codes usable as {@code ability_mod("…")} args (STR, DEX, …). */
    private List<String> abilityCodes;
}
