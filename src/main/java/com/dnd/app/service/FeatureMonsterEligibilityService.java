package com.dnd.app.service;

import com.dnd.app.domain.CreatureType;
import com.dnd.app.domain.Monster;
import com.dnd.app.domain.featurerule.FeatureAllowedMonsterFilter;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.dto.featurerule.MonsterEligibilityResult;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.MonsterRepository;
import com.dnd.app.service.formula.FormulaContext;
import com.dnd.app.service.formula.FormulaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Класс FeatureMonsterEligibilityService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureMonsterEligibilityService {

    private final MonsterRepository monsterRepository;
    private final FeatureFormulaRepository formulaRepository;
    private final FeatureFormulaService formulaService;

    /**
     * Выполняет операции "check" в рамках бизнес-логики домена.
     * @param monsterId идентификатор monster, используемый для выбора нужного бизнес-объекта
     * @param filter входящее значение filter, используемое бизнес-сценарием
     * @param ctx входящее значение ctx, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public MonsterEligibilityResult check(UUID monsterId, FeatureAllowedMonsterFilter filter, FormulaContext ctx) {
        Monster monster = monsterRepository.findById(monsterId).orElse(null);
        if (monster == null) {
            return ineligible(monsterId, "Монстр не найден");
        }
        Set<String> typeCodes = monster.getCreatureTypes() == null ? Set.of()
                : monster.getCreatureTypes().stream()
                        .map(CreatureType::getCode).filter(c -> c != null).collect(Collectors.toSet());
        Integer maxCr = (filter != null && filter.getMaxCrFormulaId() != null)
                ? evalInt(filter.getMaxCrFormulaId(), ctx) : null;
        return decide(monsterId, typeCodes, monster.getCrValue(), filter, maxCr);
    }

    /**
     * Выполняет операции "decide" в рамках бизнес-логики домена.
     * @param monsterId идентификатор monster, используемый для выбора нужного бизнес-объекта
     * @param creatureTypeCodes входящее значение creature type codes, используемое бизнес-сценарием
     * @param crValue входящее значение cr value, используемое бизнес-сценарием
     * @param filter входящее значение filter, используемое бизнес-сценарием
     * @param maxCr входящее значение max cr, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public MonsterEligibilityResult decide(UUID monsterId, Set<String> creatureTypeCodes, BigDecimal crValue,
                                           FeatureAllowedMonsterFilter filter, Integer maxCr) {
        if (filter == null) {
            return eligible(monsterId);
        }
        if (filter.getCreatureType() != null && !filter.getCreatureType().isBlank()) {
            boolean typeOk = creatureTypeCodes.stream()
                    .anyMatch(c -> c.equalsIgnoreCase(filter.getCreatureType()));
            if (!typeOk) {
                return ineligible(monsterId, "Тип существа не подходит (нужен " + filter.getCreatureType() + ")");
            }
        }
        if (maxCr != null && crValue != null && crValue.compareTo(BigDecimal.valueOf(maxCr)) > 0) {
            return ineligible(monsterId, "CR монстра выше допустимого (макс " + maxCr + ")");
        }
        return eligible(monsterId);
    }

    private MonsterEligibilityResult eligible(UUID monsterId) {
        return MonsterEligibilityResult.builder().monsterId(monsterId).eligible(true).build();
    }

    private MonsterEligibilityResult ineligible(UUID monsterId, String reason) {
        return MonsterEligibilityResult.builder().monsterId(monsterId).eligible(false).reason(reason).build();
    }

    private Integer evalInt(UUID formulaId, FormulaContext ctx) {
        FeatureFormula formula = formulaRepository.findById(formulaId).orElse(null);
        if (formula == null) {
            return null;
        }
        try {
            return formulaService.evaluateInteger(formula, ctx);
        } catch (FormulaException e) {
            log.warn("Monster CR formula failed for {}: {}", formulaId, e.getMessage());
            return null;
        }
    }
}
