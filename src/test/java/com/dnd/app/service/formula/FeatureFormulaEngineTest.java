package com.dnd.app.service.formula;

import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.dto.featurerule.FeatureFormulaEvaluateRequest;
import com.dnd.app.dto.featurerule.FeatureFormulaEvaluateResponse;
import com.dnd.app.dto.featurerule.FeatureFormulaValidationResponse;
import com.dnd.app.dto.featurerule.FormulaContextPayload;
import com.dnd.app.service.FeatureFormulaService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Unit tests for the bounded formula DSL — the Stage 3 acceptance surface (no Spring, no DB). */
class FeatureFormulaEngineTest {

    private final FeatureFormulaEvaluator evaluator = new FeatureFormulaEvaluator();
    private final FeatureFormulaService service = new FeatureFormulaService(evaluator);

    private MapFormulaContext ctx() {
        return new MapFormulaContext()
                .scalar("character_level", 8)
                .scalar("proficiency_bonus", 3)
                .scalar("spell_slot_level", 2)
                .scalar("monster_cr", 1)
                .scalar("combat_round", 2)
                .classLevel("Druid", 9)
                .abilityMod("CHA", 4)
                .abilityMod("WIS", -1)
                .resourceCount("rage", 3);
    }

    // ── Variables & scalars ──────────────────────────────────────────────────

    @Test
    void resolvesScalarsAndKeyedAccessors() {
        assertThat(evaluator.evaluate("character_level", ctx())).isEqualTo(8.0);
        assertThat(evaluator.evaluate("proficiency_bonus", ctx())).isEqualTo(3.0);
        assertThat(evaluator.evaluate("class_level(\"Druid\")", ctx())).isEqualTo(9.0);
        assertThat(evaluator.evaluate("ability_mod(\"CHA\")", ctx())).isEqualTo(4.0);
        assertThat(evaluator.evaluate("feature_resource_count(\"rage\")", ctx())).isEqualTo(3.0);
    }

    @Test
    void spellDcFormulaUsesTheSpellcastingAbilityScalar() {
        // The stored spell save DC of the S2 backfill: 8 + proficiency + the caster's casting stat.
        MapFormulaContext ctx = ctx().scalar("spellcasting_ability_mod", 4);
        assertThat(evaluator.evaluate("8 + proficiency_bonus + spellcasting_ability_mod", ctx)).isEqualTo(15.0);
    }

    @Test
    void diceMixedWithFlatEvaluatesToIntegerViaAverage() {
        // spell_healing rows can carry dice AND flat ("2d4 + 2"): average(2d4)=5, +2 → 7 (floored).
        FeatureFormula healing = FeatureFormula.builder()
                .expression("2d4 + 2").resultType("integer").roundingMode("none").build();
        assertThat(service.evaluateInteger(healing, ctx())).isEqualTo(7);
    }

    // ── Arithmetic, rounding, min/max ────────────────────────────────────────

    @Test
    void arithmeticAndRoundingFunctions() {
        assertThat(evaluator.evaluate("2 + 3 * 4", ctx())).isEqualTo(14.0);
        assertThat(evaluator.evaluate("(2 + 3) * 4", ctx())).isEqualTo(20.0);
        assertThat(evaluator.evaluate("floor(7 / 2)", ctx())).isEqualTo(3.0);
        assertThat(evaluator.evaluate("ceil(7 / 2)", ctx())).isEqualTo(4.0);
        assertThat(evaluator.evaluate("round(7 / 2)", ctx())).isEqualTo(4.0);
        assertThat(evaluator.evaluate("min(3, 5, 1)", ctx())).isEqualTo(1.0);
        assertThat(evaluator.evaluate("max(1, ability_mod(\"WIS\"))", ctx())).isEqualTo(1.0);
        assertThat(evaluator.evaluate("abs(0 - 5)", ctx())).isEqualTo(5.0);
    }

    // ── step() level tables (Rage 2→6 etc.) ──────────────────────────────────

    @Test
    void stepLevelTable() {
        // Rage uses: step(character_level, 1,2, 3,3, 6,4, 12,5, 17,6)
        String rage = "step(character_level, 1,2, 3,3, 6,4, 12,5, 17,6)";
        assertThat(evaluator.evaluate(rage, new MapFormulaContext().scalar("character_level", 1))).isEqualTo(2.0);
        assertThat(evaluator.evaluate(rage, new MapFormulaContext().scalar("character_level", 2))).isEqualTo(2.0);
        assertThat(evaluator.evaluate(rage, new MapFormulaContext().scalar("character_level", 3))).isEqualTo(3.0);
        assertThat(evaluator.evaluate(rage, new MapFormulaContext().scalar("character_level", 8))).isEqualTo(4.0);
        assertThat(evaluator.evaluate(rage, new MapFormulaContext().scalar("character_level", 20))).isEqualTo(6.0);
        // Below the first threshold → 0.
        assertThat(evaluator.evaluate("step(character_level, 5,10)", new MapFormulaContext().scalar("character_level", 4)))
                .isEqualTo(0.0);
        // Malformed (even arg count) is rejected.
        assertThatThrownBy(() -> evaluator.evaluate("step(character_level, 1)", ctx()))
                .isInstanceOf(FormulaException.class);
    }

    // ── Golden scenarios from the plan ───────────────────────────────────────

    @Test
    void wildShapeDurationFormula() {
        // floor(class_level("Druid") / 2) hours; Druid 9 -> 4
        Object result = evaluator.evaluate("floor(class_level(\"Druid\") / 2)", ctx());
        assertThat(result).isEqualTo(4.0);
    }

    @Test
    void bardicInspirationResourceScaling() {
        // max(1, ability_mod("CHA")); CHA +4 -> 4, and floored at 1 when negative
        assertThat(evaluator.evaluate("max(1, ability_mod(\"CHA\"))", ctx())).isEqualTo(4.0);
        MapFormulaContext lowCha = new MapFormulaContext().abilityMod("CHA", -2);
        assertThat(evaluator.evaluate("max(1, ability_mod(\"CHA\"))", lowCha)).isEqualTo(1.0);
    }

    // ── Dice ─────────────────────────────────────────────────────────────────

    @Test
    void diceLiteralAndFunction() {
        assertThat(evaluator.evaluate("2d6", ctx())).isEqualTo(new DiceValue(2, 6));
        assertThat(evaluator.evaluate("dice(\"1d8\")", ctx())).isEqualTo(new DiceValue(1, 8));
        assertThat(evaluator.evaluate("dice(\"d20\")", ctx())).isEqualTo(new DiceValue(1, 20));
    }

    @Test
    void diceSpecAcceptsRussianNotationAndInnerSpaces() {
        // Русская нотация «8к6»/«8д6» и пробелы внутри записи — легитимный пользовательский ввод.
        assertThat(evaluator.evaluate("dice(\"8к6\")", ctx())).isEqualTo(new DiceValue(8, 6));
        assertThat(evaluator.evaluate("dice(\"8Д6\")", ctx())).isEqualTo(new DiceValue(8, 6));
        assertThat(evaluator.evaluate("dice(\"8 d 6\")", ctx())).isEqualTo(new DiceValue(8, 6));
    }

    @Test
    void diceSpecRejectsMalformedWithFormatHint() {
        // Кириллическая «т» («1000т1000») — опечатка, не нотация: ошибка обязана подсказывать формат.
        assertThatThrownBy(() -> evaluator.evaluate("dice(\"1000т1000\")", ctx()))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("ожидается запись NdM");
        assertThatThrownBy(() -> evaluator.evaluate("dice(\"0d6\")", ctx()))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("ожидается запись NdM");
    }

    @Test
    void diceNotationNormalizeIsSafeForFullFormulas() {
        assertThat(DiceNotation.normalize("2к8 + wis_mod")).isEqualTo("2d8 + wis_mod");
        assertThat(DiceNotation.normalize("8 Д 6")).isEqualTo("8d6");
        assertThat(DiceNotation.normalize("dex_mod + 1")).isEqualTo("dex_mod + 1");
        assertThat(DiceNotation.normalize(null)).isNull();
    }

    @Test
    void diceCapsAcceptRealDiceAndScalars() {
        // Реальные кости и безкостёвые скаляры проходят капы без ошибок.
        DiceNotation.enforceDiceCaps("8d6");
        DiceNotation.enforceDiceCaps("2d8 + wis_mod");
        DiceNotation.enforceDiceCaps("dex_mod + 1");
        DiceNotation.enforceDiceCaps(null);
        DiceNotation.enforceDiceCaps("");
    }

    @Test
    void diceCapsRejectTooManyDice() {
        // «1000d1000» — синтаксически валидно, но количество и грани вне реальных костей.
        assertThatThrownBy(() -> DiceNotation.enforceDiceCaps("1000d1000"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Слишком много костей");
        assertThatThrownBy(() -> DiceNotation.enforceDiceCaps("41d6"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Слишком много костей");
    }

    @Test
    void diceCapsRejectUnrealDie() {
        // d1000 / d7 — не игровая кость.
        assertThatThrownBy(() -> DiceNotation.enforceDiceCaps("2d7"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Недопустимая кость");
        assertThatThrownBy(() -> DiceNotation.enforceDiceCaps("1d1000"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Недопустимая кость");
    }

    // ── Boolean predicates ───────────────────────────────────────────────────

    @Test
    void booleanPredicates() {
        assertThat(evaluator.evaluate("character_level >= 5", ctx())).isEqualTo(true);
        assertThat(evaluator.evaluate("character_level < 5", ctx())).isEqualTo(false);
        assertThat(evaluator.evaluate("character_level >= 5 && ability_mod(\"CHA\") > 0", ctx())).isEqualTo(true);
        assertThat(evaluator.evaluate("character_level < 5 || proficiency_bonus == 3", ctx())).isEqualTo(true);
        assertThat(evaluator.evaluate("!(character_level == 8)", ctx())).isEqualTo(false);
    }

    // ── Required-context static analysis ─────────────────────────────────────

    @Test
    void collectsRequiredContext() {
        assertThat(evaluator.requiredContext("floor(class_level(\"Druid\") / 2) + proficiency_bonus"))
                .containsExactlyInAnyOrder("class_level(Druid)", "proficiency_bonus");
    }

    // ── Negative cases ───────────────────────────────────────────────────────

    @Test
    void rejectsUnknownVariable() {
        assertThatThrownBy(() -> evaluator.evaluate("foo + 1", ctx()))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("Неизвестная переменная");
    }

    @Test
    void rejectsForbiddenFunction() {
        assertThatThrownBy(() -> evaluator.evaluate("sqrt(4)", ctx()))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("Запрещённая функция");
    }

    @Test
    void rejectsDivisionByZero() {
        assertThatThrownBy(() -> evaluator.evaluate("5 / 0", ctx()))
                .isInstanceOf(FormulaException.class)
                .hasMessageContaining("Деление на ноль");
    }

    @Test
    void rejectsTypeMismatchInArithmetic() {
        assertThatThrownBy(() -> evaluator.evaluate("character_level + (1 == 1)", ctx()))
                .isInstanceOf(FormulaException.class);
    }

    @Test
    void rejectsSyntaxError() {
        assertThatThrownBy(() -> evaluator.evaluate("2 +", ctx()))
                .isInstanceOf(FormulaException.class);
    }

    // ── Service-level validation & typed evaluation ──────────────────────────

    @Test
    void serviceValidatesResultType() {
        FeatureFormulaValidationResponse ok = service.validate("floor(class_level(\"Druid\") / 2)", "integer");
        assertThat(ok.isValid()).isTrue();
        assertThat(ok.getRequiredContext()).contains("class_level(Druid)");

        FeatureFormulaValidationResponse wrongType = service.validate("character_level >= 5", "integer");
        assertThat(wrongType.isValid()).isFalse();

        FeatureFormulaValidationResponse booleanOk = service.validate("character_level >= 5", "boolean");
        assertThat(booleanOk.isValid()).isTrue();

        FeatureFormulaValidationResponse unknownVar = service.validate("foo", "integer");
        assertThat(unknownVar.isValid()).isFalse();
    }

    @Test
    void servicePreviewEvaluatesWithContextAndRoundingAndClamp() {
        FeatureFormulaEvaluateRequest req = FeatureFormulaEvaluateRequest.builder()
                .expression("class_level(\"Druid\") / 2")
                .resultType("integer")
                .roundingMode("floor")
                .minValue(1.0)
                .maxValue(10.0)
                .context(FormulaContextPayload.builder()
                        .classLevels(Map.of("Druid", 9.0))
                        .build())
                .build();
        FeatureFormulaEvaluateResponse res = service.evaluatePreview(req);
        assertThat(res.isOk()).isTrue();
        assertThat(res.getNumericValue()).isEqualTo(4.0);
        assertThat(res.getDisplayValue()).isEqualTo("4");
    }

    @Test
    void servicePreviewReportsErrorsGracefully() {
        FeatureFormulaEvaluateRequest req = FeatureFormulaEvaluateRequest.builder()
                .expression("5 / 0")
                .resultType("integer")
                .build();
        FeatureFormulaEvaluateResponse res = service.evaluatePreview(req);
        assertThat(res.isOk()).isFalse();
        assertThat(res.getMessage()).contains("Деление на ноль");
    }
}
