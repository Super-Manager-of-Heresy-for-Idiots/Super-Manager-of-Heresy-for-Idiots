package com.dnd.app.service;

import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FormulaResultType;
import com.dnd.app.domain.featurerule.FormulaRoundingMode;
import com.dnd.app.dto.featurerule.FeatureFormulaEvaluateRequest;
import com.dnd.app.dto.featurerule.FeatureFormulaEvaluateResponse;
import com.dnd.app.dto.featurerule.FeatureFormulaValidationResponse;
import com.dnd.app.dto.featurerule.FormulaContextPayload;
import com.dnd.app.service.formula.DiceValue;
import com.dnd.app.service.formula.FeatureFormulaEvaluator;
import com.dnd.app.service.formula.FormulaContext;
import com.dnd.app.service.formula.FormulaException;
import com.dnd.app.service.formula.MapFormulaContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Класс FeatureFormulaService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Service
@RequiredArgsConstructor
public class FeatureFormulaService {

    private final FeatureFormulaEvaluator evaluator;

    // ── Validation ──────────────────────────────────────────────────────────

    /**
     * Проверяет корректность операции "validate" в рамках бизнес-логики домена.
     * @param expression входящее значение expression, используемое бизнес-сценарием
     * @param resultTypeCode входящее значение result type code, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public FeatureFormulaValidationResponse validate(String expression, String resultTypeCode) {
        FormulaResultType type = FormulaResultType.fromCode(resultTypeCode).orElse(null);
        if (type == null) {
            return FeatureFormulaValidationResponse.builder()
                    .valid(false).message("Неизвестный тип результата: " + resultTypeCode)
                    .requiredContext(List.of()).build();
        }
        try {
            List<String> required = new ArrayList<>(evaluator.requiredContext(expression));
            Object result = evaluator.evaluate(expression, probeContext());
            boolean matches = matchesType(result, type);
            return FeatureFormulaValidationResponse.builder()
                    .valid(matches)
                    .message(matches ? "OK"
                            : "Результат имеет тип «" + typeOf(result) + "», ожидался «" + type.getCode() + "»")
                    .requiredContext(required)
                    .sampleResult(display(result))
                    .build();
        } catch (FormulaException e) {
            return FeatureFormulaValidationResponse.builder()
                    .valid(false).message(e.getMessage()).requiredContext(List.of()).build();
        }
    }

    /**
     * Проверяет корректность операции "validate and stamp" в рамках бизнес-логики домена.
     * @param formula входящее значение formula, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public FeatureFormula validateAndStamp(FeatureFormula formula) {
        FeatureFormulaValidationResponse result = validate(formula.getExpression(), formula.getResultType());
        formula.setValidationStatus(result.isValid() ? "valid" : "invalid");
        formula.setValidationMessage(result.getMessage());
        if (result.getRequiredContext() != null) {
            formula.setContextRequirements(String.join(",", result.getRequiredContext()));
        }
        return formula;
    }

    // ── Preview ─────────────────────────────────────────────────────────────

    /**
     * Выполняет операции "evaluate preview" в рамках бизнес-логики домена.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @return результат выполнения бизнес-операции
     */
    public FeatureFormulaEvaluateResponse evaluatePreview(FeatureFormulaEvaluateRequest request) {
        FormulaResultType type = FormulaResultType.fromCode(request.getResultType()).orElse(null);
        if (type == null) {
            return error("Неизвестный тип результата: " + request.getResultType(), request.getResultType());
        }
        try {
            Object result = evaluator.evaluate(request.getExpression(), toContext(request.getContext()));
            if (!matchesType(result, type)) {
                return error("Результат имеет тип «" + typeOf(result) + "», ожидался «" + type.getCode() + "»",
                        request.getResultType());
            }
            FormulaRoundingMode rounding = FormulaRoundingMode.fromCode(request.getRoundingMode())
                    .orElse(FormulaRoundingMode.NONE);
            return coerce(result, type, rounding, request.getMinValue(), request.getMaxValue());
        } catch (FormulaException e) {
            return error(e.getMessage(), request.getResultType());
        }
    }

    // ── Typed evaluation (used by later stages) ─────────────────────────────

    /**
     * Выполняет операции "evaluate integer" в рамках бизнес-логики домена.
     * @param formula входящее значение formula, используемое бизнес-сценарием
     * @param ctx входящее значение ctx, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public int evaluateInteger(FeatureFormula formula, FormulaContext ctx) {
        double v = numeric(formula.getExpression(), ctx);
        return (int) clamp(roundForInteger(v, roundingOf(formula)), formula.getMinValue(), formula.getMaxValue());
    }

    /**
     * Выполняет операции "evaluate integer" в рамках бизнес-логики домена.
     * @param expression входящее значение expression, используемое бизнес-сценарием
     * @param ctx входящее значение ctx, используемое бизнес-сценарием
     * @param rounding входящее значение rounding, используемое бизнес-сценарием
     * @param min входящее значение min, используемое бизнес-сценарием
     * @param max входящее значение max, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public int evaluateInteger(String expression, FormulaContext ctx, FormulaRoundingMode rounding,
                               Double min, Double max) {
        double v = numeric(expression, ctx);
        return (int) clamp(roundForInteger(v, rounding), min, max);
    }

    /**
     * Выполняет операции "evaluate duration" в рамках бизнес-логики домена.
     * @param formula входящее значение formula, используемое бизнес-сценарием
     * @param ctx входящее значение ctx, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public int evaluateDuration(FeatureFormula formula, FormulaContext ctx) {
        return evaluateInteger(formula, ctx);
    }

    /**
     * Выполняет операции "evaluate boolean" в рамках бизнес-логики домена.
     * @param formula входящее значение formula, используемое бизнес-сценарием
     * @param ctx входящее значение ctx, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public boolean evaluateBoolean(FeatureFormula formula, FormulaContext ctx) {
        Object result = evaluator.evaluate(formula.getExpression(), ctx);
        if (result instanceof Boolean b) {
            return b;
        }
        throw new FormulaException("Ожидалось логическое значение, получено «" + typeOf(result) + "»");
    }

    /**
     * Выполняет операции "evaluate dice" в рамках бизнес-логики домена.
     * @param formula входящее значение formula, используемое бизнес-сценарием
     * @param ctx входящее значение ctx, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public DiceValue evaluateDice(FeatureFormula formula, FormulaContext ctx) {
        Object result = evaluator.evaluate(formula.getExpression(), ctx);
        if (result instanceof DiceValue d) {
            return d;
        }
        throw new FormulaException("Ожидалась кость, получено «" + typeOf(result) + "»");
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private double numeric(String expression, FormulaContext ctx) {
        Object result = evaluator.evaluate(expression, ctx);
        if (result instanceof Double d) {
            return d;
        }
        if (result instanceof DiceValue dice) {
            return dice.average();
        }
        throw new FormulaException("Ожидалось число, получено «" + typeOf(result) + "»");
    }

    private FormulaRoundingMode roundingOf(FeatureFormula formula) {
        return FormulaRoundingMode.fromCode(formula.getRoundingMode()).orElse(FormulaRoundingMode.NONE);
    }

    /** For integer results, NONE still needs to collapse to a whole number — floor by D&D convention. */
    private double roundForInteger(double v, FormulaRoundingMode rounding) {
        return rounding == FormulaRoundingMode.NONE ? Math.floor(v) : rounding.apply(v);
    }

    private double clamp(double v, Double min, Double max) {
        if (min != null && v < min) {
            v = min;
        }
        if (max != null && v > max) {
            v = max;
        }
        return v;
    }

    private FeatureFormulaEvaluateResponse coerce(Object result, FormulaResultType type,
                                                  FormulaRoundingMode rounding, Double min, Double max) {
        return switch (type) {
            case BOOLEAN -> FeatureFormulaEvaluateResponse.builder()
                    .ok(true).resultType(type.getCode())
                    .booleanValue((Boolean) result)
                    .displayValue(String.valueOf(result))
                    .build();
            case DICE -> {
                DiceValue d = (DiceValue) result;
                yield FeatureFormulaEvaluateResponse.builder()
                        .ok(true).resultType(type.getCode())
                        .diceValue(d.toExpression())
                        .displayValue(d.toExpression())
                        .build();
            }
            case DECIMAL -> {
                double v = clamp((Double) result, min, max);
                yield FeatureFormulaEvaluateResponse.builder()
                        .ok(true).resultType(type.getCode())
                        .numericValue(v).displayValue(trimNumber(v))
                        .build();
            }
            case INTEGER, DURATION, MODIFIER -> {
                double v = clamp(roundForInteger((Double) result, rounding), min, max);
                long asLong = (long) v;
                yield FeatureFormulaEvaluateResponse.builder()
                        .ok(true).resultType(type.getCode())
                        .numericValue((double) asLong).displayValue(Long.toString(asLong))
                        .build();
            }
        };
    }

    private FormulaContext toContext(FormulaContextPayload payload) {
        MapFormulaContext ctx = new MapFormulaContext();
        if (payload == null) {
            return ctx;
        }
        putScalars(payload.getScalars(), ctx::scalar);
        putScalars(payload.getClassLevels(), ctx::classLevel);
        putScalars(payload.getAbilityMods(), ctx::abilityMod);
        putScalars(payload.getResourceCounts(), ctx::resourceCount);
        if (payload.getTargetConditions() != null) {
            payload.getTargetConditions().forEach((k, v) -> {
                if (k != null && v != null) {
                    ctx.targetCondition(k, v);
                }
            });
        }
        return ctx;
    }

    private void putScalars(Map<String, Double> map, java.util.function.BiConsumer<String, Double> sink) {
        if (map != null) {
            map.forEach((k, v) -> {
                if (k != null && v != null) {
                    sink.accept(k, v);
                }
            });
        }
    }

    private boolean matchesType(Object result, FormulaResultType type) {
        if (type == FormulaResultType.BOOLEAN) {
            return result instanceof Boolean;
        }
        if (type == FormulaResultType.DICE) {
            return result instanceof DiceValue;
        }
        return result instanceof Double; // integer, decimal, duration, modifier
    }

    private String typeOf(Object result) {
        if (result instanceof Boolean) {
            return "boolean";
        }
        if (result instanceof DiceValue) {
            return "dice";
        }
        if (result instanceof Double) {
            return "number";
        }
        return "unknown";
    }

    private String display(Object result) {
        if (result instanceof DiceValue d) {
            return d.toExpression();
        }
        if (result instanceof Double d) {
            return trimNumber(d);
        }
        return String.valueOf(result);
    }

    private String trimNumber(double v) {
        if (v == Math.rint(v) && !Double.isInfinite(v)) {
            return Long.toString((long) v);
        }
        return Double.toString(v);
    }

    private FeatureFormulaEvaluateResponse error(String message, String resultType) {
        return FeatureFormulaEvaluateResponse.builder()
                .ok(false).message(message).resultType(resultType).build();
    }

    private FormulaContext probeContext() {
        return new FormulaContext() {
            @Override public Double scalar(String name) { return 1.0; }
            @Override public Double classLevel(String classKey) { return 1.0; }
            @Override public Double abilityMod(String abilityKey) { return 1.0; }
            @Override public Double featureResourceCount(String resourceKey) { return 1.0; }
            @Override public Boolean targetCondition(String conditionKey) { return true; }
        };
    }
}
