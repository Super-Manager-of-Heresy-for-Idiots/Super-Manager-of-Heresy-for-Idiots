package com.dnd.app.service;

import com.dnd.app.domain.BuffDebuff;
import com.dnd.app.domain.CharacterActiveEffect;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.content.SpeciesTraitEffect;
import com.dnd.app.dto.response.CharacterRaceSnapshotResponse;
import com.dnd.app.repository.SpeciesTraitEffectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dnd.app.domain.featurerule.ActiveEffectStatus;
import com.dnd.app.domain.featurerule.EffectStackingPolicy;
import com.dnd.app.domain.featurerule.FeatureActiveEffect;
import com.dnd.app.domain.featurerule.FeatureEffectDefinition;
import com.dnd.app.domain.featurerule.FeatureEffectModifier;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.dto.combat.AppliedModifier;
import com.dnd.app.dto.combat.ModifierTarget;
import com.dnd.app.repository.CharacterActiveEffectRepository;
import com.dnd.app.repository.FeatureActiveEffectRepository;
import com.dnd.app.repository.FeatureEffectDefinitionRepository;
import com.dnd.app.repository.FeatureEffectModifierRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.service.formula.CharacterFormulaContextFactory;
import com.dnd.app.service.formula.FormulaContext;
import com.dnd.app.service.formula.FormulaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The single place that answers "what modifies X for this character?", reading BOTH effect systems:
 * the legacy {@code buffs_debuffs} (via {@code character_active_effects}) and the feature-rules
 * effects ({@code feature_effect_modifier} via {@code character_active_effect}, whose value formulas
 * are finally evaluated here). Modifiers sharing a {@code stackKey} do not stack — the aggregator
 * keeps the maximum — which closes the "+2 from an item and the same +2 as a feature = +4" trap while
 * two genuinely different sources still sum.
 *
 * <p>Source B modifier-type vocabulary is defined here (it had no consumer before): {@code ac_bonus},
 * {@code attack_bonus}, {@code damage_bonus}, {@code save_bonus}, {@code check_bonus},
 * {@code initiative_bonus}, {@code stat_bonus} (affects check/save/initiative), and for
 * {@link #damageMultiplier} the {@code damage_resistance}/{@code damage_vulnerability} flags. Unknown
 * types are ignored so bad data can never silently change a number.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModifierAggregator {

    private static final String ACTIVE = ActiveEffectStatus.ACTIVE.getCode();
    private static final String STAT_MODIFIER = "STAT_MODIFIER";

    private final CharacterActiveEffectRepository characterActiveEffectRepository;
    private final FeatureActiveEffectRepository featureActiveEffectRepository;
    private final FeatureEffectModifierRepository featureEffectModifierRepository;
    private final FeatureEffectDefinitionRepository featureEffectDefinitionRepository;
    private final FeatureFormulaRepository formulaRepository;
    private final FeatureFormulaService formulaService;
    private final CharacterFormulaContextFactory contextFactory;
    private final PlayerCharacterRepository characterRepository;
    private final SpeciesTraitEffectRepository speciesTraitEffectRepository;
    private final ObjectMapper objectMapper;

    /** Every modifier (both sources) contributing to {@code target}, before the stacking rule. */
    @Transactional(readOnly = true)
    public List<AppliedModifier> modifiersFor(UUID characterId, ModifierTarget target) {
        List<AppliedModifier> out = new ArrayList<>(collectLegacy(characterId, target));
        out.addAll(collectFeature(characterId, target));
        return out;
    }

    /** Net modifier from BOTH sources for {@code target}, applying the non-stacking (max) rule. */
    @Transactional(readOnly = true)
    public int totalFor(UUID characterId, ModifierTarget target) {
        return sumWithStacking(modifiersFor(characterId, target));
    }

    /**
     * Net modifier from the FEATURE source only. For callers (like the legacy ability-check path) that
     * already sum the legacy buffs natively — adding this gives them feature-effect contributions
     * without double-counting the legacy ones.
     */
    @Transactional(readOnly = true)
    public int featureTotal(UUID characterId, ModifierTarget target) {
        return sumWithStacking(collectFeature(characterId, target));
    }

    /**
     * Damage multiplier for an incoming damage type from feature resistances/vulnerabilities AND the
     * character's racial resistances ({@code species_trait_effect}): {@code 0.5} if resistant, {@code 2.0}
     * if vulnerable, {@code 1.0} if neither or both (they cancel per RAW). A feature resistance/vulnerability
     * with no damage type applies to all types.
     */
    @Transactional(readOnly = true)
    public double damageMultiplier(UUID characterId, UUID damageTypeId) {
        List<FeatureEffectModifier> mods = activeFeatureModifiers(characterId);
        boolean resist = false;
        boolean vulnerable = false;
        for (FeatureEffectModifier mod : mods) {
            if (!damageTypeApplies(mod, damageTypeId)) {
                continue;
            }
            String type = normalize(mod.getModifierType());
            if (isResistance(type)) {
                resist = true;
            } else if (isVulnerability(type)) {
                vulnerable = true;
            }
        }
        if (!resist && hasRaceResistance(characterId, damageTypeId)) {
            resist = true; // Source C: racial resistance
        }
        if (resist == vulnerable) {
            return 1.0; // neither, or both cancel
        }
        return resist ? 0.5 : 2.0;
    }

    /**
     * Whether the character's species grants resistance to the given damage type (Source C). The species is
     * read from the character's race snapshot ({@code raceId}); its {@code resistance} trait effects are
     * matched by the reference damage type — the same table feature resistances use, so no bridge is needed.
     */
    private boolean hasRaceResistance(UUID characterId, UUID damageTypeId) {
        if (damageTypeId == null) {
            return false;
        }
        PlayerCharacter character = characterRepository.findById(characterId).orElse(null);
        if (character == null || character.getRaceSnapshotJson() == null
                || character.getRaceSnapshotJson().isBlank()) {
            return false;
        }
        UUID speciesId;
        try {
            CharacterRaceSnapshotResponse snapshot = objectMapper.readValue(
                    character.getRaceSnapshotJson(), CharacterRaceSnapshotResponse.class);
            speciesId = snapshot.getRaceId();
        } catch (Exception e) {
            log.warn("Race snapshot parse failed for character {}: {}", characterId, e.getMessage());
            return false;
        }
        if (speciesId == null) {
            return false;
        }
        for (SpeciesTraitEffect effect
                : speciesTraitEffectRepository.findBySpeciesIdAndEffectType(speciesId, "resistance")) {
            if (effect.getDamageType() != null && damageTypeId.equals(effect.getDamageType().getId())) {
                return true;
            }
        }
        return false;
    }

    // ── Source A: legacy buffs_debuffs ────────────────────────────────────────

    private List<AppliedModifier> collectLegacy(UUID characterId, ModifierTarget target) {
        if (!isStatScoped(target.kind())) {
            return List.of(); // legacy buffs only model stat modifiers
        }
        List<AppliedModifier> out = new ArrayList<>();
        for (CharacterActiveEffect effect : characterActiveEffectRepository.findByCharacterId(characterId)) {
            BuffDebuff bd = effect.getBuffDebuff();
            if (bd == null || !STAT_MODIFIER.equals(bd.getEffectType()) || bd.getModifierValue() == null) {
                continue;
            }
            StatType stat = bd.getTargetStat();
            if (stat == null || !statMatches(target, stat)) {
                continue;
            }
            int value = Boolean.TRUE.equals(bd.getIsBuff()) ? bd.getModifierValue() : -bd.getModifierValue();
            String key = target.kind() + "|" + statKey(target, stat) + "|buff:" + bd.getName();
            out.add(new AppliedModifier(value, "buff:" + bd.getName(), key));
        }
        return out;
    }

    private boolean statMatches(ModifierTarget target, StatType stat) {
        if (target.statTypeId() != null && target.statTypeId().equals(stat.getId())) {
            return true;
        }
        return target.statSlug() != null && target.statSlug().equalsIgnoreCase(stat.getSlug());
    }

    private String statKey(ModifierTarget target, StatType stat) {
        if (stat.getSlug() != null) {
            return stat.getSlug();
        }
        return target.statTypeId() != null ? target.statTypeId().toString() : String.valueOf(stat.getId());
    }

    // ── Source B: feature effects ─────────────────────────────────────────────

    private List<AppliedModifier> collectFeature(UUID characterId, ModifierTarget target) {
        List<FeatureActiveEffect> effects = featureActiveEffectRepository.findByCharacterIdAndStatus(characterId, ACTIVE);
        if (effects.isEmpty()) {
            return List.of();
        }
        List<UUID> defIds = effects.stream().map(FeatureActiveEffect::getEffectDefinitionId).distinct().toList();
        Map<UUID, List<FeatureEffectModifier>> modsByDef = featureEffectModifierRepository.findByEffectDefinitionIdIn(defIds)
                .stream().collect(Collectors.groupingBy(FeatureEffectModifier::getEffectDefinitionId));
        Map<UUID, FeatureEffectDefinition> defs = featureEffectDefinitionRepository.findAllById(defIds).stream()
                .collect(Collectors.toMap(FeatureEffectDefinition::getId, d -> d));

        List<AppliedModifier> out = new ArrayList<>();
        FormulaContext ctx = null;
        for (FeatureActiveEffect effect : effects) {
            FeatureEffectDefinition def = defs.get(effect.getEffectDefinitionId());
            for (FeatureEffectModifier mod : modsByDef.getOrDefault(effect.getEffectDefinitionId(), List.of())) {
                if (!contributesTo(mod, target)) {
                    continue;
                }
                if (ctx == null) {
                    ctx = buildContext(characterId);
                    if (ctx == null) {
                        return out; // character gone; nothing more we can evaluate
                    }
                }
                int value = evalFormula(mod.getValueFormulaId(), ctx);
                String source = "feature:" + (effect.getSourceFeatureId() != null
                        ? effect.getSourceFeatureId() : effect.getId());
                out.add(new AppliedModifier(value, source, featureStackKey(target, def, effect)));
            }
        }
        return out;
    }

    private boolean contributesTo(FeatureEffectModifier mod, ModifierTarget target) {
        String type = normalize(mod.getModifierType());
        if (isStatBonus(type)) {
            return isStatScoped(target.kind()) && featureStatMatches(mod, target);
        }
        ModifierTarget.Kind mapped = kindForType(type);
        if (mapped != target.kind()) {
            return false;
        }
        if (target.kind() == ModifierTarget.Kind.STAT_CHECK || target.kind() == ModifierTarget.Kind.SAVE) {
            return featureStatMatches(mod, target);
        }
        return true;
    }

    /** A feature modifier with no ability_id applies to any stat; otherwise the target stat must match by id. */
    private boolean featureStatMatches(FeatureEffectModifier mod, ModifierTarget target) {
        if (mod.getAbilityId() == null) {
            return true;
        }
        return target.statTypeId() != null && target.statTypeId().equals(mod.getAbilityId());
    }

    private String featureStackKey(ModifierTarget target, FeatureEffectDefinition def, FeatureActiveEffect effect) {
        String defKey = def != null && def.getEffectKey() != null
                ? def.getEffectKey() : String.valueOf(effect.getEffectDefinitionId());
        String base = target.kind() + "|" + scopeKey(target) + "|feature:" + defKey;
        boolean stacks = def != null && EffectStackingPolicy.STACK.getCode().equalsIgnoreCase(def.getStackingPolicy());
        // Stacking effects each get a unique key (they sum); non-stacking share a key (max wins).
        return stacks ? base + "|" + effect.getId() : base;
    }

    private String scopeKey(ModifierTarget target) {
        if (target.statSlug() != null) {
            return target.statSlug();
        }
        if (target.statTypeId() != null) {
            return target.statTypeId().toString();
        }
        return target.damageTypeId() != null ? target.damageTypeId().toString() : "-";
    }

    private List<FeatureEffectModifier> activeFeatureModifiers(UUID characterId) {
        List<FeatureActiveEffect> effects = featureActiveEffectRepository.findByCharacterIdAndStatus(characterId, ACTIVE);
        if (effects.isEmpty()) {
            return List.of();
        }
        List<UUID> defIds = effects.stream().map(FeatureActiveEffect::getEffectDefinitionId).distinct().toList();
        return featureEffectModifierRepository.findByEffectDefinitionIdIn(defIds);
    }

    private boolean damageTypeApplies(FeatureEffectModifier mod, UUID damageTypeId) {
        if (mod.getDamageTypeId() == null) {
            return true; // untyped resistance/vulnerability applies to all damage
        }
        return damageTypeId != null && mod.getDamageTypeId().equals(damageTypeId);
    }

    private FormulaContext buildContext(UUID characterId) {
        PlayerCharacter character = characterRepository.findById(characterId).orElse(null);
        return character != null ? contextFactory.build(character) : null;
    }

    private int evalFormula(UUID formulaId, FormulaContext ctx) {
        if (formulaId == null) {
            return 0;
        }
        FeatureFormula formula = formulaRepository.findById(formulaId).orElse(null);
        if (formula == null) {
            return 0;
        }
        try {
            return formulaService.evaluateInteger(formula, ctx);
        } catch (FormulaException e) {
            log.warn("Modifier value formula {} failed: {}", formulaId, e.getMessage());
            return 0;
        }
    }

    // ── shared ────────────────────────────────────────────────────────────────

    /** Group by stackKey, take the max per group (non-stacking rule), sum the groups. */
    private int sumWithStacking(List<AppliedModifier> modifiers) {
        if (modifiers.isEmpty()) {
            return 0;
        }
        Map<String, Integer> maxByKey = new java.util.HashMap<>();
        for (AppliedModifier m : modifiers) {
            maxByKey.merge(m.stackKey(), m.value(), Math::max);
        }
        return maxByKey.values().stream().mapToInt(Integer::intValue).sum();
    }

    private boolean isStatScoped(ModifierTarget.Kind kind) {
        return kind == ModifierTarget.Kind.STAT_CHECK
                || kind == ModifierTarget.Kind.SAVE
                || kind == ModifierTarget.Kind.INITIATIVE;
    }

    private static String normalize(String type) {
        return type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
    }

    private ModifierTarget.Kind kindForType(String type) {
        return switch (type) {
            case "ac_bonus", "ac", "armor_class" -> ModifierTarget.Kind.AC;
            case "attack_bonus", "attack_roll", "attack" -> ModifierTarget.Kind.ATTACK_ROLL;
            case "damage_bonus", "damage_dealt", "damage" -> ModifierTarget.Kind.DAMAGE_DEALT;
            case "save_bonus", "saving_throw_bonus", "save" -> ModifierTarget.Kind.SAVE;
            case "check_bonus", "ability_check_bonus", "check" -> ModifierTarget.Kind.STAT_CHECK;
            case "initiative_bonus", "initiative" -> ModifierTarget.Kind.INITIATIVE;
            default -> null;
        };
    }

    private boolean isStatBonus(String type) {
        return "stat_bonus".equals(type) || "ability_score_bonus".equals(type) || "ability_bonus".equals(type);
    }

    private boolean isResistance(String type) {
        return "damage_resistance".equals(type) || "resistance".equals(type) || "resist".equals(type);
    }

    private boolean isVulnerability(String type) {
        return "damage_vulnerability".equals(type) || "vulnerability".equals(type) || "vulnerable".equals(type);
    }
}
