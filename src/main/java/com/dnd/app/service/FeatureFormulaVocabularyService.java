package com.dnd.app.service;

import com.dnd.app.dto.featurerule.FormulaVocabEntry;
import com.dnd.app.dto.featurerule.FormulaVocabularyResponse;
import com.dnd.app.service.formula.FeatureFormulaEvaluator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Builds the formula DSL vocabulary served to the admin autocomplete. The set of NAMES is taken straight from
 * {@link FeatureFormulaEvaluator}'s allowlist (single source of truth — a function the evaluator doesn't accept
 * never appears as a suggestion, and a newly added one shows up automatically), while signatures/descriptions
 * are editorial metadata attached here.
 */
@Service
public class FeatureFormulaVocabularyService {

    private static final List<String> ABILITY_CODES = List.of("STR", "DEX", "CON", "INT", "WIS", "CHA");

    /** name → [signature, argKind|null, description-ru, description-en]. */
    private static final Map<String, String[]> META = Map.ofEntries(
            Map.entry("character_level", new String[]{"character_level", null,
                    "Уровень персонажа", "Character level"}),
            Map.entry("proficiency_bonus", new String[]{"proficiency_bonus", null,
                    "Бонус мастерства", "Proficiency bonus"}),
            Map.entry("spell_slot_level", new String[]{"spell_slot_level", null,
                    "Уровень использованной ячейки заклинания", "Level of the spell slot used"}),
            Map.entry("monster_cr", new String[]{"monster_cr", null,
                    "Показатель опасности монстра (CR)", "Monster challenge rating"}),
            Map.entry("combat_round", new String[]{"combat_round", null,
                    "Номер раунда боя", "Current combat round"}),
            Map.entry("floor", new String[]{"floor(x)", null,
                    "Округление вниз", "Round down"}),
            Map.entry("ceil", new String[]{"ceil(x)", null,
                    "Округление вверх", "Round up"}),
            Map.entry("round", new String[]{"round(x)", null,
                    "Округление до ближайшего", "Round to nearest"}),
            Map.entry("abs", new String[]{"abs(x)", null,
                    "Модуль числа", "Absolute value"}),
            Map.entry("min", new String[]{"min(a, b, …)", null,
                    "Наименьшее из значений", "Smallest of the values"}),
            Map.entry("max", new String[]{"max(a, b, …)", null,
                    "Наибольшее из значений", "Largest of the values"}),
            Map.entry("step", new String[]{"step(value, t1,v1, t2,v2, …)", null,
                    "Ступенчатая таблица: результат порога, не превышающего value (для таблиц по уровням)",
                    "Step table: value of the highest threshold ≤ value (for by-level tables)"}),
            Map.entry("dice", new String[]{"dice(\"2d6\")", "dice",
                    "Кость из строки (например, \"1d8\")", "Dice from a string (e.g. \"1d8\")"}),
            Map.entry("class_level", new String[]{"class_level(\"barbarian\")", "class",
                    "Уровень в указанном классе", "Level in the named class"}),
            Map.entry("ability_mod", new String[]{"ability_mod(\"STR\")", "ability",
                    "Модификатор характеристики", "Ability modifier"}),
            Map.entry("feature_resource_count", new String[]{"feature_resource_count(\"rage\")", "resource_key",
                    "Текущее количество ресурса по ключу", "Current count of a resource by key"}),
            Map.entry("target_condition", new String[]{"target_condition(\"prone\")", "target",
                    "Состояние цели (истина/ложь)", "Target condition (true/false)"}));

    public FormulaVocabularyResponse vocabulary(String lang) {
        boolean ru = lang == null || lang.toLowerCase().startsWith("ru");

        List<FormulaVocabEntry> functions = FeatureFormulaEvaluator.functionNames().stream()
                .map(name -> entry(name, "function", name + "(", ru))
                .toList();
        List<FormulaVocabEntry> scalars = FeatureFormulaEvaluator.scalarNames().stream()
                .map(name -> entry(name, "scalar", name, ru))
                .toList();

        return FormulaVocabularyResponse.builder()
                .functions(functions)
                .scalars(scalars)
                .abilityCodes(ABILITY_CODES)
                .build();
    }

    private FormulaVocabEntry entry(String name, String kind, String insertText, boolean ru) {
        String[] m = META.get(name);
        String signature = m != null ? m[0] : ("function".equals(kind) ? name + "(…)" : name);
        String argKind = m != null ? m[1] : null;
        String description = m != null ? (ru ? m[2] : m[3]) : name;
        return FormulaVocabEntry.builder()
                .name(name)
                .kind(kind)
                .insertText(insertText)
                .signature(signature)
                .argKind(argKind)
                .description(description)
                .build();
    }
}
