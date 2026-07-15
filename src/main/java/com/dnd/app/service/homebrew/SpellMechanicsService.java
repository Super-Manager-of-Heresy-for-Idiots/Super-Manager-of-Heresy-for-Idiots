package com.dnd.app.service.homebrew;

import com.dnd.app.domain.DamageType;
import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.Spell;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.featurerule.FeatureDamageRule;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureHealingRule;
import com.dnd.app.domain.featurerule.FeatureResolutionRule;
import com.dnd.app.domain.featurerule.FeatureReviewStatus;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.domain.featurerule.FeatureRuleSource;
import com.dnd.app.dto.request.HomebrewSpellRequest;
import com.dnd.app.dto.response.HomebrewSpellResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.DamageTypeRepository;
import com.dnd.app.repository.FeatureDamageRuleRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.FeatureHealingRuleRepository;
import com.dnd.app.repository.FeatureResolutionRuleRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import com.dnd.app.repository.FeatureRuleRevisionRepository;
import com.dnd.app.repository.StatTypeRepository;
import com.dnd.app.service.FeatureFormulaService;
import com.dnd.app.service.FeatureRuleRevisionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Механика homebrew-заклинания (P2-1 Phase B). Транслирует простой GM-ввод (кости урона + тип + спасбросок /
 * формула лечения) в approved SPELL-owned {@code feature_rules}, которые исполняет движок
 * ({@code CombatFeatureExecutionService.planForSpell}). Легаси-таблицы 056–062 НЕ трогаются (мораторий).
 * Правила пакета пересобираются на каждый sync (идемпотентно), поэтому round-trip через {@link #read} обязателен.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpellMechanicsService {

    /** Формула DC спасброска заклинания (как у ванильного бэкфилла). */
    private static final String SPELL_DC_EXPRESSION = "8 + proficiency_bonus + spellcasting_ability_mod";
    private static final String OWNER_SPELL = FeatureRuleOwnerType.SPELL.getCode();
    private static final Pattern PURE_DICE = Pattern.compile("(?i)\\s*\\d+\\s*d\\s*\\d+\\s*");
    private static final Pattern DICE_WRAP = Pattern.compile("(?i)dice\\(\"(.+)\"\\)");

    private final FeatureRuleRepository ruleRepository;
    private final FeatureDamageRuleRepository damageRuleRepository;
    private final FeatureHealingRuleRepository healingRuleRepository;
    private final FeatureResolutionRuleRepository resolutionRuleRepository;
    private final FeatureRuleRevisionRepository revisionRepository;
    private final FeatureFormulaRepository formulaRepository;
    private final FeatureFormulaService formulaService;
    private final FeatureRuleRevisionService revisionService;
    private final DamageTypeRepository damageTypeRepository;
    private final StatTypeRepository statTypeRepository;

    /**
     * Пересобирает механику заклинания из запроса: удаляет прежние homebrew-правила этого заклинания и создаёт
     * заново (спасбросок → урон → лечение). Пустые поля механики = у заклинания её нет.
     * @param spell сохранённое заклинание (id есть)
     * @param pkg пакет-владелец
     * @param request тело с полями механики
     * @param username автор (для ревизий/approve)
     */
    public void sync(Spell spell, HomebrewPackage pkg, HomebrewSpellRequest request, String username) {
        clearHomebrewRules(spell, pkg.getId());

        boolean hasDamage = notBlank(request.getDamageDice());
        boolean hasSave = notBlank(request.getSaveAbility());
        boolean hasHealing = notBlank(request.getHealingFormula());

        UUID saveRuleId = null;
        if (hasSave) {
            FeatureRule sca = createRule(spell, pkg, "save_check_attack", 0,
                    "Спасбросок " + request.getSaveAbility().toUpperCase(Locale.ROOT), username);
            FeatureResolutionRule res = FeatureResolutionRule.builder()
                    .featureRuleId(sca.getId())
                    .resolutionType("saving_throw")
                    .abilityId(resolveAbility(request.getSaveAbility()))
                    .dcFormulaId(formula(SPELL_DC_EXPRESSION, "scalar", "integer"))
                    .build();
            saveRuleId = resolutionRuleRepository.save(res).getId();
            approve(sca, username);
        }

        if (hasDamage) {
            UUID diceFormulaId = formula("dice(\"" + request.getDamageDice().trim() + "\")", "dice", "dice");
            UUID damageTypeId = notBlank(request.getDamageType()) ? resolveDamageType(request.getDamageType()) : null;
            FeatureRule dmg = createRule(spell, pkg, "damage", 1,
                    "Урон " + request.getDamageDice(), username);
            FeatureDamageRule dr = FeatureDamageRule.builder()
                    .featureRuleId(dmg.getId())
                    .diceFormulaId(diceFormulaId)
                    .damageTypeId(damageTypeId)
                    .requiresAttackHit(Boolean.TRUE.equals(request.getRequiresAttackHit()))
                    .requiresSave(hasSave)
                    .halfOnSave(hasSave && Boolean.TRUE.equals(request.getHalfOnSave()))
                    .saveRuleId(saveRuleId)
                    .build();
            damageRuleRepository.save(dr);
            approve(dmg, username);
        }

        if (hasHealing) {
            UUID amountFormulaId = healingFormulaId(request.getHealingFormula().trim());
            FeatureRule heal = createRule(spell, pkg, "healing", 2,
                    "Лечение " + request.getHealingFormula(), username);
            FeatureHealingRule hr = FeatureHealingRule.builder()
                    .featureRuleId(heal.getId())
                    .amountFormulaId(amountFormulaId)
                    .tempHp(false)
                    .canReviveFromZero(false)
                    .build();
            healingRuleRepository.save(hr);
            approve(heal, username);
        }
        log.info("Homebrew spell mechanics synced: spellId={}, damage={}, save={}, healing={}",
                spell.getId(), hasDamage, hasSave, hasHealing);
    }

    /**
     * Реконструирует поля механики из SPELL-owned feature_rules заклинания в DTO ответа (для round-trip формы).
     * @param spell заклинание
     * @param resp билдер ответа (мутируется)
     */
    public void read(Spell spell, HomebrewSpellResponse resp) {
        UUID pkgId = spell.getHomebrew() != null ? spell.getHomebrew().getId() : null;
        if (pkgId == null) {
            return;
        }
        List<FeatureRule> rules = ruleRepository
                .findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(OWNER_SPELL, spell.getId());
        for (FeatureRule rule : rules) {
            if (!pkgId.equals(rule.getHomebrewPackId())) {
                continue;
            }
            if ("damage".equals(rule.getRuleType())) {
                damageRuleRepository.findByFeatureRuleId(rule.getId()).stream().findFirst().ifPresent(dr -> {
                    resp.setDamageDice(unwrapDice(expressionOf(dr.getDiceFormulaId())));
                    resp.setDamageType(slugOfDamageType(dr.getDamageTypeId()));
                    resp.setRequiresAttackHit(dr.isRequiresAttackHit());
                    resp.setHalfOnSave(dr.isHalfOnSave());
                    if (dr.getSaveRuleId() != null) {
                        resolutionRuleRepository.findById(dr.getSaveRuleId())
                                .ifPresent(res -> resp.setSaveAbility(slugOfAbility(res.getAbilityId())));
                    }
                });
            } else if ("healing".equals(rule.getRuleType())) {
                healingRuleRepository.findByFeatureRuleId(rule.getId()).stream().findFirst()
                        .ifPresent(hr -> resp.setHealingFormula(unwrapDice(expressionOf(hr.getAmountFormulaId()))));
            } else if ("save_check_attack".equals(rule.getRuleType()) && resp.getSaveAbility() == null) {
                resolutionRuleRepository.findByFeatureRuleId(rule.getId()).stream()
                        .filter(r -> "saving_throw".equals(r.getResolutionType())).findFirst()
                        .ifPresent(res -> resp.setSaveAbility(slugOfAbility(res.getAbilityId())));
            }
        }
    }

    // ================= write helpers =================

    private void clearHomebrewRules(Spell spell, UUID packageId) {
        List<FeatureRule> rules = ruleRepository
                .findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(OWNER_SPELL, spell.getId());
        for (FeatureRule rule : rules) {
            if (!packageId.equals(rule.getHomebrewPackId())) {
                continue; // не трогаем ванильные/чужие правила
            }
            // Разрываем циркулярный FK rule↔revision перед удалением.
            rule.setApprovedRevisionId(null);
            rule.setCurrentRevisionId(null);
            ruleRepository.saveAndFlush(rule);

            damageRuleRepository.deleteAll(damageRuleRepository.findByFeatureRuleId(rule.getId()));
            healingRuleRepository.deleteAll(healingRuleRepository.findByFeatureRuleId(rule.getId()));
            resolutionRuleRepository.deleteAll(resolutionRuleRepository.findByFeatureRuleId(rule.getId()));
            revisionRepository.deleteAll(revisionRepository.findByFeatureRuleIdOrderByRevisionNumberDesc(rule.getId()));
            ruleRepository.delete(rule);
        }
        ruleRepository.flush();
    }

    private FeatureRule createRule(Spell spell, HomebrewPackage pkg, String ruleType, int sortOrder,
                                   String notes, String username) {
        FeatureRule rule = FeatureRule.builder()
                .ownerType(OWNER_SPELL)
                .ownerId(spell.getId())
                .ruleType(ruleType)
                .enabled(true)
                .reviewStatus(FeatureReviewStatus.NEEDS_REVIEW.getCode())
                .source(FeatureRuleSource.SEED.getCode())
                .confidence(1.0)
                .sortOrder(sortOrder)
                .notes(notes)
                .homebrewPackId(pkg.getId())
                .build();
        rule = ruleRepository.save(rule);
        revisionService.createInitialDraft(rule, username);
        return rule;
    }

    private void approve(FeatureRule rule, String username) {
        revisionService.approveCurrent(rule.getId(), "homebrew spell mechanics", username);
    }

    private UUID formula(String expression, String expressionType, String resultType) {
        FeatureFormula f = FeatureFormula.builder()
                .expression(expression)
                .expressionType(expressionType)
                .resultType(resultType)
                .validationStatus("unknown")
                .build();
        formulaService.validateAndStamp(f);
        if ("invalid".equalsIgnoreCase(f.getValidationStatus())) {
            throw new BadRequestException("Некорректная формула «" + expression + "»: " + f.getValidationMessage());
        }
        return formulaRepository.save(f).getId();
    }

    private UUID healingFormulaId(String healing) {
        if (PURE_DICE.matcher(healing).matches()) {
            return formula("dice(\"" + healing.replaceAll("\\s+", "") + "\")", "dice", "dice");
        }
        return formula(healing, "scalar", "integer");
    }

    private UUID resolveAbility(String slug) {
        String norm = slug.toLowerCase(Locale.ROOT);
        return statTypeRepository.findByHomebrewIsNull().stream()
                .filter(st -> norm.equals(st.getSlug() == null ? null : st.getSlug().toLowerCase(Locale.ROOT)))
                .map(StatType::getId).findFirst()
                .orElseThrow(() -> new BadRequestException("Неизвестная характеристика спасброска: " + slug));
    }

    private UUID resolveDamageType(String slug) {
        return damageTypeRepository.findBySlugAndHomebrewIsNull(slug.toLowerCase(Locale.ROOT))
                .map(DamageType::getId)
                .orElseThrow(() -> new BadRequestException("Неизвестный тип урона: " + slug));
    }

    // ================= read helpers =================

    private String expressionOf(UUID formulaId) {
        if (formulaId == null) {
            return null;
        }
        return formulaRepository.findById(formulaId).map(FeatureFormula::getExpression).orElse(null);
    }

    private String unwrapDice(String expression) {
        if (expression == null) {
            return null;
        }
        var m = DICE_WRAP.matcher(expression.trim());
        return m.matches() ? m.group(1) : expression;
    }

    private String slugOfDamageType(UUID id) {
        if (id == null) {
            return null;
        }
        return damageTypeRepository.findById(id).map(DamageType::getSlug).orElse(null);
    }

    private String slugOfAbility(UUID id) {
        if (id == null) {
            return null;
        }
        Map<UUID, String> byId = new HashMap<>();
        statTypeRepository.findByHomebrewIsNull().forEach(st -> byId.put(st.getId(), st.getSlug()));
        return byId.get(id);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
