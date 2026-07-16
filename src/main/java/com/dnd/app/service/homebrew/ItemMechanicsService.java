package com.dnd.app.service.homebrew;

import com.dnd.app.domain.DamageType;
import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.featurerule.FeatureDamageRule;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureHealingRule;
import com.dnd.app.domain.featurerule.FeatureItemBinding;
import com.dnd.app.domain.featurerule.FeatureResolutionRule;
import com.dnd.app.domain.featurerule.FeatureReviewStatus;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.domain.featurerule.FeatureRuleSource;
import com.dnd.app.dto.request.HomebrewItemRequest;
import com.dnd.app.dto.response.HomebrewItemResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.DamageTypeRepository;
import com.dnd.app.repository.FeatureDamageRuleRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.FeatureHealingRuleRepository;
import com.dnd.app.repository.FeatureItemBindingRepository;
import com.dnd.app.repository.FeatureResolutionRuleRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import com.dnd.app.repository.FeatureRuleRevisionRepository;
import com.dnd.app.repository.StatTypeRepository;
import com.dnd.app.service.FeatureFormulaService;
import com.dnd.app.service.FeatureRuleRevisionService;
import com.dnd.app.service.formula.DiceNotation;
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
 * Механика homebrew-предмета (P1.5 / IT-4, Фаза 4). Транслирует GM-ввод (кости урона + тип + спасбросок / формула
 * лечения + гейтинг binding'ом: экипировка/аттюнмент/расход) в approved feature_rules с owner_type=ITEM_MAGIC|ITEM_EQUIPMENT,
 * которые исполняет боевой движок (`ItemAbilityUseService`), гейт `items-enabled`. Каждое item-правило ОБЯЗАНО иметь
 * `feature_item_binding` (инвариант валидатора). Легаси-таблицы предметов не трогаются; идемпотентный resync + round-trip read.
 * Приём зеркалит {@link SpellMechanicsService}, добавляя binding и переменный owner-тип.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemMechanicsService {

    private static final String SPELL_DC_EXPRESSION = "8 + proficiency_bonus + spellcasting_ability_mod";
    private static final Pattern PURE_DICE = Pattern.compile("(?i)\\s*\\d+\\s*d\\s*\\d+\\s*");
    private static final Pattern DICE_WRAP = Pattern.compile("(?i)dice\\(\"(.+)\"\\)");

    private final FeatureRuleRepository ruleRepository;
    private final FeatureDamageRuleRepository damageRuleRepository;
    private final FeatureHealingRuleRepository healingRuleRepository;
    private final FeatureResolutionRuleRepository resolutionRuleRepository;
    private final FeatureItemBindingRepository itemBindingRepository;
    private final FeatureRuleRevisionRepository revisionRepository;
    private final FeatureFormulaRepository formulaRepository;
    private final FeatureFormulaService formulaService;
    private final FeatureRuleRevisionService revisionService;
    private final DamageTypeRepository damageTypeRepository;
    private final StatTypeRepository statTypeRepository;

    /**
     * Пересобирает механику предмета: удаляет прежние homebrew-правила этого предмета и создаёт заново
     * (спасбросок → урон → лечение), каждое с {@code feature_item_binding}. Пустые поля механики = у предмета её нет.
     * @param ownerType owner-тип правил (ITEM_MAGIC | ITEM_EQUIPMENT), по виду предмета
     * @param itemId идентификатор предмета-определения (magic_item.id / equipment_item.id)
     * @param pkg пакет-владелец
     * @param request тело с полями механики/binding
     * @param username автор
     */
    public void sync(FeatureRuleOwnerType ownerType, UUID itemId, HomebrewPackage pkg,
                     HomebrewItemRequest request, String username) {
        String ownerCode = ownerType.getCode();
        clearHomebrewRules(ownerCode, itemId, pkg.getId());

        boolean hasDamage = notBlank(request.getAbilityDamageDice());
        boolean hasSave = notBlank(request.getAbilitySaveAbility());
        boolean hasHealing = notBlank(request.getAbilityHealingFormula());
        if (!hasDamage && !hasSave && !hasHealing) {
            return;
        }

        UUID saveRuleId = null;
        if (hasSave) {
            FeatureRule sca = createRule(ownerCode, itemId, "save_check_attack", 0,
                    "Спасбросок " + request.getAbilitySaveAbility().toUpperCase(Locale.ROOT), pkg, username);
            FeatureResolutionRule res = FeatureResolutionRule.builder()
                    .featureRuleId(sca.getId())
                    .resolutionType("saving_throw")
                    .abilityId(resolveAbility(request.getAbilitySaveAbility()))
                    .dcFormulaId(formula(SPELL_DC_EXPRESSION, "scalar", "integer"))
                    .build();
            saveRuleId = resolutionRuleRepository.save(res).getId();
            bind(sca, request);
            approve(sca, username);
        }

        if (hasDamage) {
            String normDamage = DiceNotation.normalize(request.getAbilityDamageDice().trim());
            DiceNotation.enforceDiceCaps(normDamage);
            UUID diceFormulaId = formula("dice(\"" + normDamage + "\")", "dice", "dice");
            UUID damageTypeId = notBlank(request.getAbilityDamageType())
                    ? resolveDamageType(request.getAbilityDamageType()) : null;
            FeatureRule dmg = createRule(ownerCode, itemId, "damage", 1,
                    "Урон " + request.getAbilityDamageDice(), pkg, username);
            FeatureDamageRule dr = FeatureDamageRule.builder()
                    .featureRuleId(dmg.getId())
                    .diceFormulaId(diceFormulaId)
                    .damageTypeId(damageTypeId)
                    .requiresAttackHit(false)
                    .requiresSave(hasSave)
                    .halfOnSave(hasSave && Boolean.TRUE.equals(request.getAbilityHalfOnSave()))
                    .saveRuleId(saveRuleId)
                    .build();
            damageRuleRepository.save(dr);
            bind(dmg, request);
            approve(dmg, username);
        }

        if (hasHealing) {
            UUID amountFormulaId = healingFormulaId(request.getAbilityHealingFormula().trim());
            FeatureRule heal = createRule(ownerCode, itemId, "healing", 2,
                    "Лечение " + request.getAbilityHealingFormula(), pkg, username);
            FeatureHealingRule hr = FeatureHealingRule.builder()
                    .featureRuleId(heal.getId())
                    .amountFormulaId(amountFormulaId)
                    .tempHp(false)
                    .canReviveFromZero(false)
                    .build();
            healingRuleRepository.save(hr);
            bind(heal, request);
            approve(heal, username);
        }
        log.info("Homebrew item mechanics synced: ownerType={}, itemId={}, damage={}, save={}, healing={}",
                ownerCode, itemId, hasDamage, hasSave, hasHealing);
    }

    /**
     * Реконструирует поля механики предмета из его ITEM-owned feature_rules в DTO ответа (для round-trip формы).
     * @param ownerType owner-тип правил
     * @param itemId идентификатор предмета
     * @param pkgId пакет-владелец
     * @param resp билдер ответа (мутируется)
     */
    public void read(FeatureRuleOwnerType ownerType, UUID itemId, UUID pkgId, HomebrewItemResponse resp) {
        if (pkgId == null) {
            return;
        }
        List<FeatureRule> rules = ruleRepository
                .findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(ownerType.getCode(), itemId);
        for (FeatureRule rule : rules) {
            if (!pkgId.equals(rule.getHomebrewPackId())) {
                continue;
            }
            itemBindingRepository.findByFeatureRuleId(rule.getId()).ifPresent(b -> {
                resp.setAbilityRequiresEquipped(b.isRequiresEquipped());
                resp.setAbilityRequiresAttunement(b.isRequiresAttunement());
                resp.setAbilityConsumeOnUse(b.isConsumeOnUse());
            });
            if ("damage".equals(rule.getRuleType())) {
                damageRuleRepository.findByFeatureRuleId(rule.getId()).stream().findFirst().ifPresent(dr -> {
                    resp.setAbilityDamageDice(unwrapDice(expressionOf(dr.getDiceFormulaId())));
                    resp.setAbilityDamageType(slugOfDamageType(dr.getDamageTypeId()));
                    resp.setAbilityHalfOnSave(dr.isHalfOnSave());
                    if (dr.getSaveRuleId() != null) {
                        resolutionRuleRepository.findById(dr.getSaveRuleId())
                                .ifPresent(res -> resp.setAbilitySaveAbility(slugOfAbility(res.getAbilityId())));
                    }
                });
            } else if ("healing".equals(rule.getRuleType())) {
                healingRuleRepository.findByFeatureRuleId(rule.getId()).stream().findFirst()
                        .ifPresent(hr -> resp.setAbilityHealingFormula(unwrapDice(expressionOf(hr.getAmountFormulaId()))));
            } else if ("save_check_attack".equals(rule.getRuleType()) && resp.getAbilitySaveAbility() == null) {
                resolutionRuleRepository.findByFeatureRuleId(rule.getId()).stream()
                        .filter(r -> "saving_throw".equals(r.getResolutionType())).findFirst()
                        .ifPresent(res -> resp.setAbilitySaveAbility(slugOfAbility(res.getAbilityId())));
            }
        }
    }

    /**
     * Удаляет все homebrew-правила механики предмета (при удалении предмета из пакета).
     * @param ownerType owner-тип правил (ITEM_MAGIC | ITEM_EQUIPMENT)
     * @param itemId идентификатор предмета
     * @param packageId пакет-владелец
     */
    public void clear(FeatureRuleOwnerType ownerType, UUID itemId, UUID packageId) {
        clearHomebrewRules(ownerType.getCode(), itemId, packageId);
    }

    // ================= write helpers =================

    private void clearHomebrewRules(String ownerCode, UUID itemId, UUID packageId) {
        List<FeatureRule> rules = ruleRepository
                .findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(ownerCode, itemId);
        for (FeatureRule rule : rules) {
            if (!packageId.equals(rule.getHomebrewPackId())) {
                continue;
            }
            rule.setApprovedRevisionId(null);
            rule.setCurrentRevisionId(null);
            ruleRepository.saveAndFlush(rule);

            itemBindingRepository.findByFeatureRuleId(rule.getId()).ifPresent(itemBindingRepository::delete);
            damageRuleRepository.deleteAll(damageRuleRepository.findByFeatureRuleId(rule.getId()));
            healingRuleRepository.deleteAll(healingRuleRepository.findByFeatureRuleId(rule.getId()));
            resolutionRuleRepository.deleteAll(resolutionRuleRepository.findByFeatureRuleId(rule.getId()));
            revisionRepository.deleteAll(revisionRepository.findByFeatureRuleIdOrderByRevisionNumberDesc(rule.getId()));
            ruleRepository.delete(rule);
        }
        ruleRepository.flush();
    }

    private FeatureRule createRule(String ownerCode, UUID itemId, String ruleType, int sortOrder,
                                   String notes, HomebrewPackage pkg, String username) {
        FeatureRule rule = FeatureRule.builder()
                .ownerType(ownerCode)
                .ownerId(itemId)
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
        revisionService.approveCurrent(rule.getId(), "homebrew item ability", username);
    }

    private void bind(FeatureRule rule, HomebrewItemRequest request) {
        FeatureItemBinding binding = FeatureItemBinding.builder()
                .featureRuleId(rule.getId())
                .requiresEquipped(Boolean.TRUE.equals(request.getAbilityRequiresEquipped()))
                .requiresAttunement(Boolean.TRUE.equals(request.getAbilityRequiresAttunement()))
                .consumeOnUse(Boolean.TRUE.equals(request.getAbilityConsumeOnUse()))
                .consumeQuantity(1)
                .build();
        // consume_on_use несовместим с requires_equipped (инвариант валидатора) — расход имеет приоритет.
        if (binding.isConsumeOnUse()) {
            binding.setRequiresEquipped(false);
        }
        itemBindingRepository.save(binding);
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
        // Нормализуем русскую дайс-нотацию («2к8») до классификации: иначе чистые кости уйдут в scalar-парсер.
        String norm = DiceNotation.normalize(healing);
        DiceNotation.enforceDiceCaps(norm);
        if (PURE_DICE.matcher(norm).matches()) {
            return formula("dice(\"" + norm.replaceAll("\\s+", "") + "\")", "dice", "dice");
        }
        return formula(norm, "scalar", "integer");
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
