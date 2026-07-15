package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.ItemInstance;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.featurerule.CharacterFeatureResource;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureResourceDefinition;
import com.dnd.app.domain.featurerule.FeatureResourceScope;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.domain.featurerule.FeatureSpellFilter;
import com.dnd.app.domain.featurerule.FeatureSpellGrant;
import com.dnd.app.domain.featurerule.ItemInstanceFeatureResource;
import com.dnd.app.dto.featurerule.FeatureSpellCastResult;
import com.dnd.app.dto.featurerule.FeatureSpellGrantResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CharacterFeatureResourceRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.FeatureResourceDefinitionRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import com.dnd.app.repository.FeatureSpellFilterRepository;
import com.dnd.app.repository.FeatureSpellGrantRepository;
import com.dnd.app.repository.ItemInstanceFeatureResourceRepository;
import com.dnd.app.repository.ItemInstanceRepository;
import com.dnd.app.service.formula.CharacterFormulaContextFactory;
import com.dnd.app.service.formula.FormulaContext;
import com.dnd.app.service.formula.FormulaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureSpellGrantService {

    private final FeatureRulesProperties flags;
    private final CharacterFeatureResolver resolver;
    private final FeatureSpellGrantRepository grantRepository;
    private final FeatureSpellFilterRepository filterRepository;
    private final FeatureFormulaRepository formulaRepository;
    private final FeatureFormulaService formulaService;
    private final CharacterFormulaContextFactory contextFactory;
    private final CharacterFeatureResourceRepository resourceRepository;
    private final FeatureResourceDefinitionRepository resourceDefinitionRepository;
    private final FeatureResourceService featureResourceService;
    private final FeatureRuleRepository ruleRepository;
    private final ItemAbilityResolver itemAbilityResolver;
    private final ItemAbilityProvisioningService itemAbilityProvisioningService;
    private final ItemInstanceRepository itemInstanceRepository;
    private final ItemInstanceFeatureResourceRepository itemResourceRepository;

    /**
     * Возвращает заклинания, дарованные персонажу: классовыми фичами (source=class) и,
     * при активной item-механике, предметами (source=item, с id/именем экземпляра).
     * @param character персонаж, для которого собираются гранты
     * @return список грантов заклинаний из всех источников
     */
    @Transactional(readOnly = true)
    public List<FeatureSpellGrantResponse> listGrantedSpells(PlayerCharacter character) {
        List<FeatureSpellGrantResponse> out = new java.util.ArrayList<>();
        if (flags.spellsActive()) {
            out.addAll(classGrantedSpells(character));
        }
        if (flags.itemsActive()) {
            out.addAll(itemGrantedSpells(character));
        }
        return out;
    }

    /**
     * Гранты заклинаний от классовых фич (owner CLASS_FEATURE).
     * @param character персонаж
     * @return список классовых грантов
     */
    private List<FeatureSpellGrantResponse> classGrantedSpells(PlayerCharacter character) {
        List<ClassFeature> features = resolver.knownBaseClassFeatures(character.getId());
        if (features.isEmpty()) {
            return List.of();
        }
        Map<UUID, ClassFeature> featureById = features.stream()
                .collect(Collectors.toMap(ClassFeature::getId, Function.identity(), (a, b) -> a));
        List<FeatureRule> rules = resolver.approvedEnabledRules(featureById.keySet());
        if (rules.isEmpty()) {
            return List.of();
        }
        Map<UUID, FeatureRule> ruleById = rules.stream()
                .collect(Collectors.toMap(FeatureRule::getId, Function.identity(), (a, b) -> a));
        List<FeatureSpellGrant> grants = grantRepository.findByFeatureRuleIdIn(ruleById.keySet());
        if (grants.isEmpty()) {
            return List.of();
        }
        FormulaContext ctx = contextFactory.build(character);

        return grants.stream().map(g -> {
            FeatureRule rule = ruleById.get(g.getFeatureRuleId());
            if (rule == null) {
                return null;
            }
            ClassFeature feature = featureById.get(rule.getOwnerId());
            if (feature == null) {
                return null;
            }
            return FeatureSpellGrantResponse.builder()
                    .id(g.getId())
                    .featureId(feature.getId())
                    .featureName(feature.getTitle())
                    .spellId(g.getSpellId())
                    .countsAgainstKnown(g.isCountsAgainstKnown())
                    .alwaysPrepared(g.isAlwaysPrepared())
                    .castWithoutSlot(g.isCastWithoutSlot())
                    .usesResourceDefinitionId(g.getUsesResourceDefinitionId())
                    .spellcastingAbilityOverrideId(g.getSpellcastingAbilityOverrideId())
                    .filter(buildFilter(g.getSpellFilterId(), ctx))
                    .source("class")
                    .build();
        }).filter(r -> r != null).toList();
    }

    /**
     * Гранты заклинаний от предметов (owner из ITEM_FAMILY). Один и тот же грант на двух
     * одинаковых предметах даёт две записи с разными {@code sourceItemInstanceId} (D3).
     * @param character персонаж-владелец инвентаря
     * @return список item-грантов с проставленным источником-предметом
     */
    private List<FeatureSpellGrantResponse> itemGrantedSpells(PlayerCharacter character) {
        List<ItemAbilityResolver.ActiveItemRule> active = itemAbilityResolver.resolveActiveRules(character);
        if (active.isEmpty()) {
            return List.of();
        }
        List<UUID> ruleIds = active.stream().map(a -> a.rule().getId()).distinct().toList();
        List<FeatureSpellGrant> grants = grantRepository.findByFeatureRuleIdIn(ruleIds);
        if (grants.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<FeatureSpellGrant>> grantsByRule = grants.stream()
                .collect(Collectors.groupingBy(FeatureSpellGrant::getFeatureRuleId));
        FormulaContext ctx = contextFactory.build(character);

        List<FeatureSpellGrantResponse> out = new java.util.ArrayList<>();
        for (ItemAbilityResolver.ActiveItemRule a : active) {
            for (FeatureSpellGrant g : grantsByRule.getOrDefault(a.rule().getId(), List.of())) {
                out.add(FeatureSpellGrantResponse.builder()
                        .id(g.getId())
                        .featureId(null)
                        .featureName(a.instance().getDisplayName())
                        .spellId(g.getSpellId())
                        .countsAgainstKnown(g.isCountsAgainstKnown())
                        .alwaysPrepared(g.isAlwaysPrepared())
                        .castWithoutSlot(g.isCastWithoutSlot())
                        .usesResourceDefinitionId(g.getUsesResourceDefinitionId())
                        .spellcastingAbilityOverrideId(g.getSpellcastingAbilityOverrideId())
                        .filter(buildFilter(g.getSpellFilterId(), ctx))
                        .source("item")
                        .sourceItemInstanceId(a.instance().getId())
                        .sourceItemName(a.instance().getDisplayName())
                        .build());
            }
        }
        return out;
    }

    /**
     * Каст заклинания через грант (классовый или item). Совместимость: без предмета.
     * @param character персонаж
     * @param grantId id гранта заклинания
     * @return результат каста
     */
    @Transactional
    public FeatureSpellCastResult castViaFeature(PlayerCharacter character, UUID grantId) {
        return castViaFeature(character, grantId, null);
    }

    /**
     * Каст заклинания через грант с опциональным источником-предметом.
     * Если грант принадлежит правилу item-семейства — списывает заряды экземпляра
     * ({@code item_instance_feature_resource}); иначе идёт классовым путём и списывает
     * character-скоуп ресурс (ITEM_ABIL Фаза 3, §4.3).
     * @param character персонаж
     * @param grantId id гранта заклинания
     * @param itemInstanceId id экземпляра предмета-источника (обязателен для item-гранта, иначе null)
     * @return результат каста
     */
    @Transactional
    public FeatureSpellCastResult castViaFeature(PlayerCharacter character, UUID grantId, UUID itemInstanceId) {
        FeatureSpellGrant grant = grantRepository.findById(grantId)
                .orElseThrow(() -> new ResourceNotFoundException("Заклинание умения не найдено: " + grantId));
        FeatureRule rule = ruleRepository.findById(grant.getFeatureRuleId())
                .orElseThrow(() -> new ResourceNotFoundException("Правило гранта не найдено"));
        boolean itemOwned = FeatureRuleOwnerType.fromCode(rule.getOwnerType())
                .map(t -> t.isItemOwner()).orElse(false);
        if (itemOwned) {
            return castViaItem(character, grant, rule, itemInstanceId);
        }

        if (!flags.spellsActive()) {
            throw new BadRequestException("Runtime заклинаний умений отключён");
        }
        List<UUID> featureIds = resolver.knownBaseClassFeatures(character.getId()).stream()
                .map(ClassFeature::getId).toList();
        List<UUID> ruleIds = resolver.approvedEnabledRules(featureIds).stream()
                .map(FeatureRule::getId).toList();
        if (!ruleIds.contains(grant.getFeatureRuleId())) {
            throw new BadRequestException("Заклинание не принадлежит этому персонажу");
        }

        Integer spent = null;
        Integer remaining = null;
        String resourceKey = null;
        if (grant.getUsesResourceDefinitionId() != null) {
            FeatureResourceDefinition def =
                    resourceDefinitionRepository.findById(grant.getUsesResourceDefinitionId()).orElse(null);
            if (def != null) {
                CharacterFeatureResource res = findResource(character.getId(), def);
                if (res == null) {
                    throw new BadRequestException("Ресурс для каста не инициализирован");
                }
                CharacterFeatureResource updated = featureResourceService.spend(character.getId(), res.getId(), 1);
                spent = 1;
                remaining = updated.getCurrentValue();
                resourceKey = def.getResourceKey();
            }
        }

        return FeatureSpellCastResult.builder()
                .spellGrantId(grant.getId())
                .spellId(grant.getSpellId())
                .castWithoutSlot(grant.isCastWithoutSlot())
                .resourceKey(resourceKey)
                .resourceSpent(spent)
                .resourceRemaining(remaining)
                .message("Каст выполнен через умение")
                .build();
    }

    /**
     * Каст заклинания из предмета: проверяет владение и активность правила для экземпляра,
     * списывает заряды экземпляра под блокировкой. {@code cast_without_slot=true} — штатный
     * кейс палочек/свитков.
     * @param character персонаж-владелец
     * @param grant грант заклинания (spell_grant профиль item-правила)
     * @param rule item-правило, которому принадлежит грант
     * @param itemInstanceId id экземпляра предмета-источника
     * @return результат каста с проставленным {@code sourceItemInstanceId}
     */
    private FeatureSpellCastResult castViaItem(PlayerCharacter character, FeatureSpellGrant grant,
                                               FeatureRule rule, UUID itemInstanceId) {
        if (!flags.itemsActive()) {
            throw new BadRequestException("Item-механика отключена");
        }
        if (itemInstanceId == null) {
            throw new BadRequestException("itemInstanceId обязателен для каста из предмета");
        }
        ItemInstance item = itemInstanceRepository.findByIdForUpdate(itemInstanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));
        if (item.getOwnerCharacter() == null || !item.getOwnerCharacter().getId().equals(character.getId())) {
            throw new BadRequestException("Предмет не принадлежит персонажу");
        }
        boolean active = itemAbilityResolver.resolveActiveRules(item).stream()
                .anyMatch(a -> a.rule().getId().equals(rule.getId()));
        if (!active) {
            throw new DuplicateResourceException("ITEM_RULE_NOT_ACTIVE");
        }
        itemAbilityProvisioningService.ensureInstanceResources(item);

        Integer spent = null;
        Integer remaining = null;
        String resourceKey = null;
        if (grant.getUsesResourceDefinitionId() != null) {
            FeatureResourceDefinition def =
                    resourceDefinitionRepository.findById(grant.getUsesResourceDefinitionId()).orElse(null);
            if (def != null && def.getScope() == FeatureResourceScope.ITEM_INSTANCE) {
                ItemInstanceFeatureResource resource = itemResourceRepository
                        .findByItemInstanceIdAndResourceDefinitionIdForUpdate(item.getId(), def.getId())
                        .orElseThrow(() -> new BadRequestException("Ресурс предмета не инициализирован"));
                int current = resource.getCurrentValue() != null ? resource.getCurrentValue() : 0;
                int amount = 1;
                if (current < amount && !def.isAllowNegative()) {
                    throw new DuplicateResourceException("INSUFFICIENT_ITEM_CHARGES");
                }
                resource.setCurrentValue(current - amount);
                itemResourceRepository.save(resource);
                spent = amount;
                remaining = resource.getCurrentValue();
                resourceKey = def.getResourceKey();
            }
        }

        return FeatureSpellCastResult.builder()
                .spellGrantId(grant.getId())
                .spellId(grant.getSpellId())
                .castWithoutSlot(grant.isCastWithoutSlot())
                .resourceKey(resourceKey)
                .resourceSpent(spent)
                .resourceRemaining(remaining)
                .sourceItemInstanceId(item.getId())
                .message("Каст выполнен из предмета")
                .build();
    }

    private FeatureSpellGrantResponse.Filter buildFilter(UUID filterId, FormulaContext ctx) {
        if (filterId == null) {
            return null;
        }
        FeatureSpellFilter f = filterRepository.findById(filterId).orElse(null);
        if (f == null) {
            return null;
        }
        return FeatureSpellGrantResponse.Filter.builder()
                .classId(f.getClassId())
                .schoolId(f.getSchoolId())
                .maxSpellLevel(evalInt(f.getMaxSpellLevelFormulaId(), ctx))
                .tag(f.getTag())
                .sourceFilter(f.getSourceFilter())
                .build();
    }

    private CharacterFeatureResource findResource(UUID characterId, FeatureResourceDefinition def) {
        if (def.getSharedPoolKey() != null && !def.getSharedPoolKey().isBlank()) {
            return resourceRepository.findFirstByCharacterIdAndSharedPoolKey(characterId, def.getSharedPoolKey())
                    .orElse(null);
        }
        return resourceRepository.findByCharacterIdAndResourceDefinitionId(characterId, def.getId()).orElse(null);
    }

    private Integer evalInt(UUID formulaId, FormulaContext ctx) {
        if (formulaId == null) {
            return null;
        }
        FeatureFormula formula = formulaRepository.findById(formulaId).orElse(null);
        if (formula == null) {
            return null;
        }
        try {
            return formulaService.evaluateInteger(formula, ctx);
        } catch (FormulaException e) {
            log.warn("Spell filter formula failed for {}: {}", formulaId, e.getMessage());
            return null;
        }
    }
}
