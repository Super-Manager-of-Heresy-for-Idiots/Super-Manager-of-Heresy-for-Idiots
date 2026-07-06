package com.dnd.app.service;

import com.dnd.app.domain.BuffDebuff;
import com.dnd.app.domain.Spell;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.content.ContentSkill;
import com.dnd.app.domain.content.SpellDamage;
import com.dnd.app.domain.content.SpellHealing;
import com.dnd.app.domain.featurerule.ActionType;
import com.dnd.app.domain.featurerule.EffectStackingPolicy;
import com.dnd.app.domain.featurerule.FeatureActionCost;
import com.dnd.app.domain.featurerule.FeatureAttackRule;
import com.dnd.app.domain.featurerule.FeatureDamageRule;
import com.dnd.app.domain.featurerule.FeatureEffectDefinition;
import com.dnd.app.domain.featurerule.FeatureEffectModifier;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureHealingRule;
import com.dnd.app.domain.featurerule.FeatureIssueSeverity;
import com.dnd.app.domain.featurerule.FeatureResolutionRule;
import com.dnd.app.domain.featurerule.FeatureReviewStatus;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleIssue;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.domain.featurerule.FeatureRuleProfile;
import com.dnd.app.domain.featurerule.FeatureRuleSource;
import com.dnd.app.dto.featurerule.SpellRuleBackfillResult;
import com.dnd.app.repository.ActionTypeRepository;
import com.dnd.app.repository.ContentSkillRepository;
import com.dnd.app.repository.DurationUnitRepository;
import com.dnd.app.repository.FeatureActionCostRepository;
import com.dnd.app.repository.FeatureAttackRuleRepository;
import com.dnd.app.repository.FeatureDamageRuleRepository;
import com.dnd.app.repository.FeatureEffectDefinitionRepository;
import com.dnd.app.repository.FeatureEffectModifierRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.FeatureHealingRuleRepository;
import com.dnd.app.repository.FeatureResolutionRuleRepository;
import com.dnd.app.repository.FeatureRuleIssueRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import com.dnd.app.repository.SpellRepository;
import com.dnd.app.repository.StatTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * S2 (spell-stack absorption): backfills the structured spell mechanics of migrations 056–062 into
 * feature rules with {@code owner_type = SPELL}, one rule per (spell, aspect):
 *
 * <ul>
 *   <li>{@code save_ability}/{@code check_*}/{@code is_attack_roll} → a {@code save_check_attack} rule
 *       (resolution rows with the RAW spell-DC formula {@code 8 + proficiency_bonus +
 *       spellcasting_ability_mod}, plus a spell attack row);</li>
 *   <li>{@code spell_damage} → a {@code damage} rule (one damage row per source entry, dice formula);</li>
 *   <li>{@code spell_healing} → a {@code healing} rule (dice/flat amount formula);</li>
 *   <li>{@code casting_action_slug} → an {@code action_cost} rule (action/bonus_action/reaction;
 *       long casts map to {@code special}, which the combat economy treats as cost-free);</li>
 *   <li>{@code spell_buffs} → an {@code active_effect} rule ({@code buff:<name>} effect keys bridge the
 *       stacking key with the legacy buff system; duration from the spell, concentration flag).</li>
 * </ul>
 *
 * <p>The legacy tables are NOT dropped and stay the read model for the folio/battle DTOs (dual-read
 * period). Rules are {@code MIGRATION}-sourced and auto-approved: the source columns are structured,
 * admin-curated data, the same determinism argument as the background backfill. Spells whose save could
 * not be parsed ({@code is_warning}) simply have no {@code save_ability} — nothing unreliable is copied;
 * re-running the backfill after an admin resolves the warning picks the spell up. Idempotent per
 * (spell, rule type). Where the old schema cannot express a detail ({@code half_on_save}), a WARN issue
 * is attached instead of guessing silently.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpellRuleBackfillService {

    private static final String OWNER = FeatureRuleOwnerType.SPELL.getCode();
    private static final String ACTOR = "spell-backfill";

    /** RAW spell save DC, evaluated in the caster's formula context at plan/cast time. */
    static final String SPELL_DC_EXPRESSION = "8 + proficiency_bonus + spellcasting_ability_mod";

    /** spell.save_ability / check_ability canonical codes → ability_score slugs. */
    private static final Map<String, String> ABILITY_SLUGS = Map.of(
            "STRENGTH", "str", "DEXTERITY", "dex", "CONSTITUTION", "con",
            "INTELLIGENCE", "int", "WISDOM", "wis", "CHARISMA", "cha");

    /** spell.casting_action_slug → action_type code ("time" = casts longer than a turn). */
    private static final Map<String, String> ACTION_CODES = Map.of(
            "action", "action", "bonus-action", "bonus_action", "reaction", "reaction", "time", "special");

    private final SpellRepository spellRepository;
    private final FeatureRuleRepository ruleRepository;
    private final FeatureRuleIssueRepository issueRepository;
    private final FeatureRuleRevisionService revisionService;
    private final FeatureFormulaService formulaService;
    private final FeatureFormulaRepository formulaRepository;
    private final FeatureDamageRuleRepository damageRuleRepository;
    private final FeatureHealingRuleRepository healingRuleRepository;
    private final FeatureResolutionRuleRepository resolutionRuleRepository;
    private final FeatureAttackRuleRepository attackRuleRepository;
    private final FeatureActionCostRepository actionCostRepository;
    private final FeatureEffectDefinitionRepository effectDefinitionRepository;
    private final FeatureEffectModifierRepository effectModifierRepository;
    private final ActionTypeRepository actionTypeRepository;
    private final DurationUnitRepository durationUnitRepository;
    private final StatTypeRepository statTypeRepository;
    private final ContentSkillRepository skillRepository;

    /** Mutable per-run counters (the DTO is assembled at the end). */
    private static final class Pass {
        int sourceSpells;
        int sourceRows;
        int rulesCreated;
        int detailRowsCreated;
        int skippedExisting;
        int skippedNoData;

        SpellRuleBackfillResult.PassStats stats() {
            return SpellRuleBackfillResult.PassStats.builder()
                    .sourceSpells(sourceSpells).sourceRows(sourceRows)
                    .rulesCreated(rulesCreated).detailRowsCreated(detailRowsCreated)
                    .skippedExisting(skippedExisting).skippedNoData(skippedNoData)
                    .build();
        }
    }

    private static final class Run {
        final boolean apply;
        final Pass damage = new Pass();
        final Pass healing = new Pass();
        final Pass resolution = new Pass();
        final Pass actionCost = new Pass();
        final Pass effects = new Pass();
        int formulasCreated;
        int issuesCreated;

        Run(boolean apply) {
            this.apply = apply;
        }
    }

    @Transactional
    public SpellRuleBackfillResult backfill(boolean apply) {
        Run run = new Run(apply);
        Map<String, UUID> abilityIdBySlug = abilityIds();
        List<ContentSkill> skills = skillRepository.findAll();

        List<Spell> spells = spellRepository.findAll();
        for (Spell spell : spells) {
            Map<String, FeatureRule> existingByType = new HashMap<>();
            ruleRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(OWNER, spell.getId()).stream()
                    .filter(r -> FeatureRuleSource.MIGRATION.getCode().equals(r.getSource()))
                    .forEach(r -> existingByType.putIfAbsent(r.getRuleType(), r));

            // Resolution first: the damage pass links its save gate to the resolution rule.
            UUID saveRuleId = backfillResolution(run, spell, existingByType, abilityIdBySlug, skills);
            backfillDamage(run, spell, existingByType, saveRuleId);
            backfillHealing(run, spell, existingByType);
            backfillActionCost(run, spell, existingByType);
            backfillEffects(run, spell, existingByType);
        }

        if (apply) {
            log.info("Spell rule backfill: {} spells; rules created: dmg={}, heal={}, resolution={}, action={}, effects={}",
                    spells.size(), run.damage.rulesCreated, run.healing.rulesCreated,
                    run.resolution.rulesCreated, run.actionCost.rulesCreated, run.effects.rulesCreated);
        }
        return SpellRuleBackfillResult.builder()
                .applied(apply)
                .spellsTotal(spells.size())
                .damage(run.damage.stats())
                .healing(run.healing.stats())
                .resolution(run.resolution.stats())
                .actionCost(run.actionCost.stats())
                .effects(run.effects.stats())
                .formulasCreated(run.formulasCreated)
                .issuesCreated(run.issuesCreated)
                .build();
    }

    // ── Pass 1: save / check / attack ────────────────────────────────────────

    /** @return the id of the spell's save resolution rule (created now or pre-existing), or null. */
    private UUID backfillResolution(Run run, Spell spell, Map<String, FeatureRule> existing,
                                    Map<String, UUID> abilityIdBySlug, List<ContentSkill> skills) {
        boolean hasSave = notBlank(spell.getSaveAbility());
        boolean hasCheck = notBlank(spell.getCheckAbility());
        boolean hasAttack = Boolean.TRUE.equals(spell.getAttackRoll());
        if (!hasSave && !hasCheck && !hasAttack) {
            return null;
        }
        run.resolution.sourceSpells++;
        run.resolution.sourceRows += (hasSave ? 1 : 0) + (hasCheck ? 1 : 0) + (hasAttack ? 1 : 0);

        FeatureRule existingRule = existing.get(FeatureRuleProfile.SAVE_CHECK_ATTACK.getCode());
        if (existingRule != null) {
            run.resolution.skippedExisting++;
            if (!hasSave) {
                return null;
            }
            return resolutionRuleRepository.findByFeatureRuleId(existingRule.getId()).stream()
                    .filter(r -> "saving_throw".equals(r.getResolutionType()))
                    .map(FeatureResolutionRule::getId)
                    .findFirst().orElse(null);
        }
        if (!run.apply) {
            run.resolution.rulesCreated++;
            run.resolution.detailRowsCreated += (hasSave ? 1 : 0) + (hasCheck ? 1 : 0) + (hasAttack ? 1 : 0);
            run.formulasCreated += (hasSave ? 1 : 0) + (hasCheck ? 1 : 0);
            return null;
        }

        String notes = "Спасбросок/проверка/атака заклинания из колонок spell (057/061)";
        String checkSkillRaw = trimToNull(spell.getCheckSkill());
        UUID skillId = resolveSkillId(checkSkillRaw, skills);
        if (hasCheck && checkSkillRaw != null && skillId == null) {
            notes += "; навык проверки не сопоставлен: «" + checkSkillRaw + "»";
        }
        FeatureRule rule = createRule(spell, FeatureRuleProfile.SAVE_CHECK_ATTACK, 0, notes);
        run.resolution.rulesCreated++;

        UUID saveRuleId = null;
        if (hasSave) {
            FeatureResolutionRule save = resolutionRuleRepository.save(FeatureResolutionRule.builder()
                    .featureRuleId(rule.getId())
                    .resolutionType("saving_throw")
                    .abilityId(abilityId(abilityIdBySlug, spell.getSaveAbility()))
                    .dcFormulaId(dcFormula(run).getId())
                    .build());
            saveRuleId = save.getId();
            run.resolution.detailRowsCreated++;
        }
        if (hasCheck) {
            resolutionRuleRepository.save(FeatureResolutionRule.builder()
                    .featureRuleId(rule.getId())
                    .resolutionType(skillId != null ? "skill_check" : "ability_check")
                    .abilityId(abilityId(abilityIdBySlug, spell.getCheckAbility()))
                    .skillId(skillId)
                    .dcFormulaId(dcFormula(run).getId())
                    .build());
            run.resolution.detailRowsCreated++;
        }
        if (hasAttack) {
            attackRuleRepository.save(FeatureAttackRule.builder()
                    .featureRuleId(rule.getId())
                    .attackKind("spell")
                    .build());
            run.resolution.detailRowsCreated++;
        }
        approve(rule, "Детерминированный перенос резолюции заклинания (spell-stack absorption)");
        return saveRuleId;
    }

    // ── Pass 2: damage ───────────────────────────────────────────────────────

    private void backfillDamage(Run run, Spell spell, Map<String, FeatureRule> existing, UUID saveRuleId) {
        List<SpellDamage> entries = spell.getDamages();
        if (entries == null || entries.isEmpty()) {
            return;
        }
        List<SpellDamage> usable = entries.stream()
                .filter(d -> notBlank(d.getDice()))
                .toList();
        run.damage.sourceRows += entries.size();
        run.damage.skippedNoData += entries.size() - usable.size();
        if (usable.isEmpty()) {
            return;
        }
        run.damage.sourceSpells++;

        boolean requiresSave = notBlank(spell.getSaveAbility());
        FeatureRule existingRule = existing.get(FeatureRuleProfile.DAMAGE.getCode());
        if (existingRule != null) {
            run.damage.skippedExisting++;
            if (run.apply && requiresSave && saveRuleId != null) {
                // A save rule appeared after the damage rule (admin resolved a warning between runs):
                // upgrade the existing damage rows so the save gate is consistent.
                for (FeatureDamageRule dr : damageRuleRepository.findByFeatureRuleId(existingRule.getId())) {
                    if (dr.getSaveRuleId() == null) {
                        dr.setRequiresSave(true);
                        dr.setSaveRuleId(saveRuleId);
                        damageRuleRepository.save(dr);
                    }
                }
            }
            return;
        }
        if (!run.apply) {
            run.damage.rulesCreated++;
            run.damage.detailRowsCreated += usable.size();
            run.formulasCreated += usable.size();
            run.issuesCreated += requiresSave ? 1 : 0;
            return;
        }

        FeatureRule rule = createRule(spell, FeatureRuleProfile.DAMAGE, 1,
                "Урон заклинания из spell_damage (056)");
        run.damage.rulesCreated++;
        for (SpellDamage entry : usable) {
            FeatureFormula dice = diceFormula(run, entry.getDice());
            damageRuleRepository.save(FeatureDamageRule.builder()
                    .featureRuleId(rule.getId())
                    .diceFormulaId(dice.getId())
                    .damageTypeId(entry.getDamageType() != null ? entry.getDamageType().getId() : null)
                    .requiresAttackHit(Boolean.TRUE.equals(spell.getAttackRoll()))
                    .requiresSave(requiresSave)
                    .halfOnSave(false)
                    .saveRuleId(requiresSave ? saveRuleId : null)
                    .build());
            run.damage.detailRowsCreated++;
        }
        if (requiresSave) {
            // The 056–062 schema never stored the on-save outcome; don't guess "half on save" silently.
            issueRepository.save(FeatureRuleIssue.builder()
                    .ownerType(OWNER).ownerId(spell.getId())
                    .featureRuleId(rule.getId())
                    .issueType("ambiguous_parse")
                    .severity(FeatureIssueSeverity.WARN.getCode())
                    .message("half_on_save неизвестно в старой схеме — по умолчанию false, проверьте описание")
                    .resolved(false)
                    .build());
            run.issuesCreated++;
        }
        approve(rule, "Детерминированный перенос урона заклинания (spell-stack absorption)");
    }

    // ── Pass 3: healing ──────────────────────────────────────────────────────

    private void backfillHealing(Run run, Spell spell, Map<String, FeatureRule> existing) {
        List<SpellHealing> entries = spell.getHealings();
        if (entries == null || entries.isEmpty()) {
            return;
        }
        run.healing.sourceSpells++;
        run.healing.sourceRows += entries.size();

        if (existing.containsKey(FeatureRuleProfile.HEALING.getCode())) {
            run.healing.skippedExisting++;
            return;
        }
        long withFormula = entries.stream().filter(h -> notBlank(h.getDice()) || h.getFlat() != null).count();
        if (!run.apply) {
            run.healing.rulesCreated++;
            run.healing.detailRowsCreated += entries.size();
            run.formulasCreated += (int) withFormula;
            return;
        }

        FeatureRule rule = createRule(spell, FeatureRuleProfile.HEALING, 2,
                "Лечение заклинания из spell_healing (060)");
        run.healing.rulesCreated++;
        for (SpellHealing entry : entries) {
            FeatureFormula amount = healingFormula(run, entry);
            healingRuleRepository.save(FeatureHealingRule.builder()
                    .featureRuleId(rule.getId())
                    .amountFormulaId(amount != null ? amount.getId() : null)
                    .build());
            run.healing.detailRowsCreated++;
        }
        approve(rule, "Детерминированный перенос лечения заклинания (spell-stack absorption)");
    }

    // ── Pass 4: action cost ──────────────────────────────────────────────────

    private void backfillActionCost(Run run, Spell spell, Map<String, FeatureRule> existing) {
        String slug = trimToNull(spell.getCastingActionSlug());
        if (slug == null) {
            return;
        }
        run.actionCost.sourceSpells++;
        run.actionCost.sourceRows++;

        if (existing.containsKey(FeatureRuleProfile.ACTION_COST.getCode())) {
            run.actionCost.skippedExisting++;
            return;
        }
        if (!run.apply) {
            run.actionCost.rulesCreated++;
            run.actionCost.detailRowsCreated++;
            return;
        }

        String mapped = ACTION_CODES.get(slug.toLowerCase(Locale.ROOT));
        if (mapped == null) {
            log.warn("Spell {}: unknown casting_action_slug '{}' mapped to 'special'", spell.getSlug(), slug);
            mapped = "special";
        }
        final String code = mapped;
        ActionType actionType = actionTypeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException("Нет action_type с кодом " + code));
        FeatureRule rule = createRule(spell, FeatureRuleProfile.ACTION_COST, 3,
                "Стоимость каста из spell.casting_action_slug = «" + slug + "»");
        run.actionCost.rulesCreated++;
        actionCostRepository.save(FeatureActionCost.builder()
                .featureRuleId(rule.getId())
                .actionTypeId(actionType.getId())
                .amount(1)
                .build());
        run.actionCost.detailRowsCreated++;
        approve(rule, "Детерминированный перенос стоимости каста (spell-stack absorption)");
    }

    // ── Pass 5: linked buffs → active effects ────────────────────────────────

    private void backfillEffects(Run run, Spell spell, Map<String, FeatureRule> existing) {
        var buffs = spell.getLinkedBuffs();
        if (buffs == null || buffs.isEmpty()) {
            return;
        }
        run.effects.sourceSpells++;
        run.effects.sourceRows += buffs.size();

        if (existing.containsKey(FeatureRuleProfile.ACTIVE_EFFECT.getCode())) {
            run.effects.skippedExisting++;
            return;
        }
        if (!run.apply) {
            run.effects.rulesCreated++;
            run.effects.detailRowsCreated += buffs.size();
            run.formulasCreated += (int) buffs.stream().filter(this::hasStatModifier).count()
                    + (int) buffs.stream().filter(b -> effectDurationValue(spell, b) != null).count();
            return;
        }

        FeatureRule rule = createRule(spell, FeatureRuleProfile.ACTIVE_EFFECT, 4,
                "Эффекты заклинания из spell_buffs (062)");
        run.effects.rulesCreated++;
        for (BuffDebuff buff : buffs) {
            String effectKey = effectKey(buff);
            Integer durationValue = effectDurationValue(spell, buff);
            String durationUnit = effectDurationUnit(spell, buff);
            FeatureFormula durationFormula = durationValue != null
                    ? intFormula(run, String.valueOf(durationValue)) : null;

            FeatureEffectDefinition def = effectDefinitionRepository.save(FeatureEffectDefinition.builder()
                    .featureRuleId(rule.getId())
                    .effectKey(effectKey)
                    .displayName(truncate(buff.getName(), 120))
                    .durationFormulaId(durationFormula != null ? durationFormula.getId() : null)
                    .durationUnitId(durationUnit != null
                            ? durationUnitRepository.findByCode(durationUnit).map(u -> u.getId()).orElse(null)
                            : null)
                    .concentrationRequired(Boolean.TRUE.equals(spell.getConcentration()))
                    .stackingPolicy(EffectStackingPolicy.REPLACE_SAME_FEATURE.getCode())
                    .build());
            run.effects.detailRowsCreated++;

            if (hasStatModifier(buff)) {
                int value = Boolean.TRUE.equals(buff.getIsBuff())
                        ? buff.getModifierValue() : -buff.getModifierValue();
                FeatureFormula valueFormula = intFormula(run, String.valueOf(value));
                effectModifierRepository.save(FeatureEffectModifier.builder()
                        .effectDefinitionId(def.getId())
                        .modifierType("stat_bonus")
                        .abilityId(buff.getTargetStat().getId())
                        .valueFormulaId(valueFormula.getId())
                        .build());
            }
        }
        approve(rule, "Детерминированный перенос бафов заклинания (spell-stack absorption)");
    }

    // ── shared helpers ───────────────────────────────────────────────────────

    private FeatureRule createRule(Spell spell, FeatureRuleProfile profile, int sortOrder, String notes) {
        FeatureRule rule = ruleRepository.save(FeatureRule.builder()
                .ownerType(OWNER).ownerId(spell.getId())
                .ruleType(profile.getCode())
                .enabled(true)
                .reviewStatus(FeatureReviewStatus.NEEDS_REVIEW.getCode())
                .source(FeatureRuleSource.MIGRATION.getCode())
                .confidence(1.0)
                .sortOrder(sortOrder)
                .notes(notes)
                .homebrewPackId(spell.getHomebrew() != null ? spell.getHomebrew().getId() : null)
                .build());
        revisionService.createInitialDraft(rule, ACTOR);
        return rule;
    }

    private void approve(FeatureRule rule, String reason) {
        revisionService.approveCurrent(rule.getId(), reason, ACTOR);
    }

    private FeatureFormula dcFormula(Run run) {
        return saveFormula(run, SPELL_DC_EXPRESSION, "scalar", "integer");
    }

    private FeatureFormula diceFormula(Run run, String dice) {
        return saveFormula(run, "dice(\"" + normalizeDice(dice) + "\")", "dice", "dice");
    }

    private FeatureFormula intFormula(Run run, String expression) {
        return saveFormula(run, expression, "scalar", "integer");
    }

    /** dice + flat → "NdM + F"; dice only → dice("NdM"); flat only → "F"; neither → null (full heal). */
    private FeatureFormula healingFormula(Run run, SpellHealing entry) {
        boolean hasDice = notBlank(entry.getDice());
        boolean hasFlat = entry.getFlat() != null;
        if (hasDice && hasFlat) {
            return saveFormula(run, normalizeDice(entry.getDice()) + " + " + entry.getFlat(),
                    "scalar", "integer");
        }
        if (hasDice) {
            return saveFormula(run, "dice(\"" + normalizeDice(entry.getDice()) + "\")", "dice", "dice");
        }
        if (hasFlat) {
            return saveFormula(run, String.valueOf(entry.getFlat()), "scalar", "integer");
        }
        return null;
    }

    private FeatureFormula saveFormula(Run run, String expression, String expressionType, String resultType) {
        FeatureFormula formula = FeatureFormula.builder()
                .expression(expression)
                .expressionType(expressionType)
                .resultType(resultType)
                .validationStatus("unknown")
                .build();
        formulaService.validateAndStamp(formula);
        run.formulasCreated++;
        return formulaRepository.save(formula);
    }

    /** Cyrillic "к" dice ("2к6") appear in legacy admin edits; the DSL only knows "d". */
    private static String normalizeDice(String dice) {
        return dice.trim().toLowerCase(Locale.ROOT).replace('к', 'd');
    }

    private Map<String, UUID> abilityIds() {
        Map<String, UUID> bySlug = new HashMap<>();
        for (StatType type : statTypeRepository.findByHomebrewIsNull()) {
            if (type.getSlug() != null) {
                bySlug.put(type.getSlug().toLowerCase(Locale.ROOT), type.getId());
            }
        }
        return bySlug;
    }

    private UUID abilityId(Map<String, UUID> abilityIdBySlug, String canonicalCode) {
        if (canonicalCode == null) {
            return null;
        }
        String slug = ABILITY_SLUGS.get(canonicalCode.trim().toUpperCase(Locale.ROOT));
        return slug != null ? abilityIdBySlug.get(slug) : null;
    }

    /** Exact RU-name match; choice texts ("Восприятие или Выживание") stay unmapped by design. */
    private UUID resolveSkillId(String checkSkillRaw, List<ContentSkill> skills) {
        if (checkSkillRaw == null || checkSkillRaw.toLowerCase(Locale.ROOT).contains(" или ")) {
            return null;
        }
        return skills.stream()
                .filter(s -> s.getNameRu() != null && s.getNameRu().equalsIgnoreCase(checkSkillRaw))
                .map(ContentSkill::getId)
                .findFirst().orElse(null);
    }

    private String effectKey(BuffDebuff buff) {
        String key = "buff:" + buff.getName();
        if (key.length() > 64) {
            log.warn("Buff name over effect-key limit, truncating: {}", buff.getName());
            key = key.substring(0, 64);
        }
        return key;
    }

    private boolean hasStatModifier(BuffDebuff buff) {
        return "STAT_MODIFIER".equals(buff.getEffectType())
                && buff.getTargetStat() != null
                && buff.getModifierValue() != null;
    }

    /** Effect duration: the spell's own duration wins; the buff's legacy rounds are the fallback. */
    private Integer effectDurationValue(Spell spell, BuffDebuff buff) {
        if (spell.getDurationAmount() != null && isTimedUnit(spell.getDurationUnit())) {
            return spell.getDurationAmount();
        }
        return buff.getDurationRounds();
    }

    private String effectDurationUnit(Spell spell, BuffDebuff buff) {
        if (spell.getDurationAmount() != null && isTimedUnit(spell.getDurationUnit())) {
            return spell.getDurationUnit().toLowerCase(Locale.ROOT);
        }
        return buff.getDurationRounds() != null ? "round" : null;
    }

    private static boolean isTimedUnit(String unit) {
        if (unit == null) {
            return false;
        }
        return switch (unit.toLowerCase(Locale.ROOT)) {
            case "round", "minute", "hour", "day" -> true;
            default -> false;
        };
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String trimToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max);
    }
}
