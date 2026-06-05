package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.DamageType;
import com.dnd.app.domain.enums.EffectRole;
import com.dnd.app.domain.enums.EquipmentSlot;
import com.dnd.app.domain.enums.SkillActivation;
import com.dnd.app.dto.request.*;
import com.dnd.app.dto.response.*;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.exception.UnprocessableEntityException;
import com.dnd.app.mapper.ReferenceDataMapper;
import com.dnd.app.mapper.UserMapper;
import com.dnd.app.repository.*;
import com.dnd.app.service.reward.RewardResolverRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final StatTypeRepository statTypeRepository;
    private final ItemTypeRepository itemTypeRepository;
    private final CharacterClassRepository classRepository;
    private final CharacterRaceRepository raceRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final SubclassRepository subclassRepository;
    private final FeatRepository featRepository;
    private final ClassLevelRewardRepository classLevelRewardRepository;
    private final SkillEffectRepository skillEffectRepository;
    private final BuffDebuffRepository buffDebuffRepository;
    private final BackgroundRepository backgroundRepository;
    private final SpellRepository spellRepository;
    private final ProficiencySkillRepository proficiencySkillRepository;
    private final ReferenceDataMapper refMapper;
    private final UserMapper userMapper;
    private final RewardResolverRegistry rewardResolverRegistry;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    // --- Stat Types ---

    @Transactional(readOnly = true)
    public List<StatTypeResponse> listStatTypes() {
        return statTypeRepository.findAll().stream().map(refMapper::toStatTypeResponse).toList();
    }

    @Transactional
    public StatTypeResponse createStatType(CreateStatTypeRequest request) {
        if (statTypeRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Характеристика с таким названием уже существует");
        }
        StatType st = StatType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .isDefault(false)
                .build();
        StatType saved = statTypeRepository.save(st);
        log.info("Admin: stat type created — name='{}', id={}", saved.getName(), saved.getId());
        return refMapper.toStatTypeResponse(saved);
    }

    @Transactional(readOnly = true)
    public StatTypeResponse getStatType(UUID id) {
        return refMapper.toStatTypeResponse(statTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Характеристика не найдена")));
    }

    @Transactional
    public StatTypeResponse updateStatType(UUID id, CreateStatTypeRequest request) {
        StatType st = statTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Характеристика не найдена"));
        if (!st.getName().equals(request.getName()) && statTypeRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Характеристика с таким названием уже существует");
        }
        st.setName(request.getName());
        st.setDescription(request.getDescription());
        return refMapper.toStatTypeResponse(statTypeRepository.save(st));
    }

    @Transactional
    public void deleteStatType(UUID id) {
        if (!statTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Характеристика не найдена");
        }
        log.info("Admin: stat type deleted — id={}", id);
        statTypeRepository.deleteById(id);
    }

    // --- Item Types ---

    @Transactional(readOnly = true)
    public List<ItemTypeResponse> listItemTypes() {
        return itemTypeRepository.findAll().stream().map(this::toItemTypeResponse).toList();
    }

    @Transactional
    public ItemTypeResponse createItemType(CreateItemTypeRequest request) {
        if (itemTypeRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Тип предмета с таким названием уже существует");
        }
        EquipmentSlot slot = parseSlot(request.getSlot());
        validateDamageFields(request.getDamageDice(), request.getDamageType());
        validateItemTypeSkillFields(request.getSkillId(), request.getSkillActivation());

        ItemType it = ItemType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .slot(slot)
                .damageDice(request.getDamageDice())
                .damageBonus(request.getDamageBonus() != null ? request.getDamageBonus() : 0)
                .damageType(parseDamageType(request.getDamageType()))
                .build();
        if (request.getSkillId() != null) {
            Skill skill = skillRepository.findById(request.getSkillId())
                    .orElseThrow(() -> new ResourceNotFoundException("Умение не найдено"));
            it.setSkill(skill);
            it.setSkillActivation(parseSkillActivation(request.getSkillActivation()));
        }
        ItemType saved = itemTypeRepository.save(it);
        log.info("Admin: item type created — name='{}', slot={}, id={}", saved.getName(), saved.getSlot(), saved.getId());
        return toItemTypeResponse(saved);
    }

    @Transactional(readOnly = true)
    public ItemTypeResponse getItemType(UUID id) {
        return toItemTypeResponse(itemTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Тип предмета не найден")));
    }

    @Transactional
    public ItemTypeResponse updateItemType(UUID id, CreateItemTypeRequest request) {
        ItemType it = itemTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Тип предмета не найден"));
        if (!it.getName().equals(request.getName()) && itemTypeRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Тип предмета с таким названием уже существует");
        }
        validateDamageFields(request.getDamageDice(), request.getDamageType());
        validateItemTypeSkillFields(request.getSkillId(), request.getSkillActivation());

        it.setName(request.getName());
        it.setDescription(request.getDescription());
        it.setSlot(parseSlot(request.getSlot()));
        it.setDamageDice(request.getDamageDice());
        it.setDamageBonus(request.getDamageBonus() != null ? request.getDamageBonus() : 0);
        it.setDamageType(parseDamageType(request.getDamageType()));
        if (request.getSkillId() != null) {
            Skill skill = skillRepository.findById(request.getSkillId())
                    .orElseThrow(() -> new ResourceNotFoundException("Умение не найдено"));
            it.setSkill(skill);
            it.setSkillActivation(parseSkillActivation(request.getSkillActivation()));
        } else {
            it.setSkill(null);
            it.setSkillActivation(null);
        }
        return toItemTypeResponse(itemTypeRepository.save(it));
    }

    @Transactional
    public void deleteItemType(UUID id) {
        if (!itemTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Тип предмета не найден");
        }
        log.info("Admin: item type deleted — id={}", id);
        itemTypeRepository.deleteById(id);
    }

    // --- Character Classes ---

    @Transactional(readOnly = true)
    public List<CharacterClassResponse> listCharacterClasses() {
        return classRepository.findAll().stream().map(refMapper::toCharacterClassResponse).toList();
    }

    @Transactional
    public CharacterClassResponse createCharacterClass(CreateCharacterClassRequest request) {
        if (classRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Класс персонажа с таким названием уже существует");
        }
        CharacterClass cc = CharacterClass.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        CharacterClass saved = classRepository.save(cc);
        log.info("Admin: character class created — name='{}', id={}", saved.getName(), saved.getId());
        return refMapper.toCharacterClassResponse(saved);
    }

    @Transactional(readOnly = true)
    public CharacterClassResponse getCharacterClass(UUID id) {
        return refMapper.toCharacterClassResponse(classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Класс персонажа не найден")));
    }

    @Transactional
    public CharacterClassResponse updateCharacterClass(UUID id, CreateCharacterClassRequest request) {
        CharacterClass cc = classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Класс персонажа не найден"));
        if (!cc.getName().equals(request.getName()) && classRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Класс персонажа с таким названием уже существует");
        }
        cc.setName(request.getName());
        cc.setDescription(request.getDescription());
        return refMapper.toCharacterClassResponse(classRepository.save(cc));
    }

    @Transactional
    public void deleteCharacterClass(UUID id) {
        if (!classRepository.existsById(id)) {
            throw new ResourceNotFoundException("Класс персонажа не найден");
        }
        log.info("Admin: character class deleted — id={}", id);
        classRepository.deleteById(id);
    }

    // --- Character Races ---

    @Transactional(readOnly = true)
    public List<CharacterRaceResponse> listCharacterRaces() {
        return raceRepository.findAll().stream().map(refMapper::toCharacterRaceResponse).toList();
    }

    @Transactional
    public CharacterRaceResponse createCharacterRace(CreateCharacterRaceRequest request) {
        if (raceRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Раса персонажа с таким названием уже существует");
        }
        CharacterRace cr = CharacterRace.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        CharacterRace saved = raceRepository.save(cr);
        log.info("Admin: character race created — name='{}', id={}", saved.getName(), saved.getId());
        return refMapper.toCharacterRaceResponse(saved);
    }

    @Transactional(readOnly = true)
    public CharacterRaceResponse getCharacterRace(UUID id) {
        return refMapper.toCharacterRaceResponse(raceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Раса персонажа не найдена")));
    }

    @Transactional
    public CharacterRaceResponse updateCharacterRace(UUID id, CreateCharacterRaceRequest request) {
        CharacterRace cr = raceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Раса персонажа не найдена"));
        if (!cr.getName().equals(request.getName()) && raceRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Раса персонажа с таким названием уже существует");
        }
        cr.setName(request.getName());
        cr.setDescription(request.getDescription());
        return refMapper.toCharacterRaceResponse(raceRepository.save(cr));
    }

    @Transactional
    public void deleteCharacterRace(UUID id) {
        if (!raceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Раса персонажа не найдена");
        }
        log.info("Admin: character race deleted — id={}", id);
        raceRepository.deleteById(id);
    }

    // --- Skills ---

    @Transactional(readOnly = true)
    public List<SkillResponse> listSkills() {
        return skillRepository.findAll().stream().map(this::toSkillResponse).toList();
    }

    @Transactional
    public SkillResponse createSkill(CreateSkillRequest request) {
        if (skillRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Умение с таким названием уже существует");
        }
        validateDamageFields(request.getDamageDice(), request.getDamageType());
        Skill skill = Skill.builder()
                .name(request.getName())
                .description(request.getDescription())
                .skillType(request.getSkillType())
                .damageDice(request.getDamageDice())
                .damageBonus(request.getDamageBonus() != null ? request.getDamageBonus() : 0)
                .damageType(parseDamageType(request.getDamageType()))
                .build();
        Skill saved = skillRepository.save(skill);
        log.info("Admin: skill created — name='{}', type={}, id={}", saved.getName(), saved.getSkillType(), saved.getId());
        return toSkillResponse(saved);
    }

    @Transactional(readOnly = true)
    public SkillResponse getSkill(UUID id) {
        return toSkillResponse(skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Умение не найдено")));
    }

    @Transactional
    public SkillResponse updateSkill(UUID id, CreateSkillRequest request) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Умение не найдено"));
        if (!skill.getName().equals(request.getName()) && skillRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Умение с таким названием уже существует");
        }
        validateDamageFields(request.getDamageDice(), request.getDamageType());
        skill.setName(request.getName());
        skill.setDescription(request.getDescription());
        skill.setSkillType(request.getSkillType());
        skill.setDamageDice(request.getDamageDice());
        skill.setDamageBonus(request.getDamageBonus() != null ? request.getDamageBonus() : 0);
        skill.setDamageType(parseDamageType(request.getDamageType()));
        return toSkillResponse(skillRepository.save(skill));
    }

    @Transactional
    public void deleteSkill(UUID id) {
        if (!skillRepository.existsById(id)) {
            throw new ResourceNotFoundException("Умение не найдено");
        }
        log.info("Admin: skill deleted — id={}", id);
        skillRepository.deleteById(id);
    }

    // --- Subclasses ---

    @Transactional(readOnly = true)
    public List<SubclassResponse> listSubclasses() {
        return subclassRepository.findAll().stream().map(this::toSubclassResponse).toList();
    }

    @Transactional
    public SubclassResponse createSubclass(CreateSubclassRequest request) {
        if (subclassRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Подкласс с таким названием уже существует");
        }
        CharacterClass parent = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Класс персонажа не найден"));
        Subclass sub = Subclass.builder()
                .name(request.getName())
                .parentClass(parent)
                .description(request.getDescription())
                .build();
        Subclass saved = subclassRepository.save(sub);
        log.info("Admin: subclass created — name='{}', parentClass='{}', id={}", saved.getName(), parent.getName(), saved.getId());
        return toSubclassResponse(saved);
    }

    @Transactional(readOnly = true)
    public SubclassResponse getSubclass(UUID id) {
        return toSubclassResponse(subclassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Подкласс не найден")));
    }

    @Transactional
    public SubclassResponse updateSubclass(UUID id, CreateSubclassRequest request) {
        Subclass sub = subclassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Подкласс не найден"));
        if (!sub.getName().equals(request.getName()) && subclassRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Подкласс с таким названием уже существует");
        }
        CharacterClass parent = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Класс персонажа не найден"));
        sub.setName(request.getName());
        sub.setParentClass(parent);
        sub.setDescription(request.getDescription());
        return toSubclassResponse(subclassRepository.save(sub));
    }

    @Transactional
    public void deleteSubclass(UUID id) {
        if (!subclassRepository.existsById(id)) {
            throw new ResourceNotFoundException("Подкласс не найден");
        }
        log.info("Admin: subclass deleted — id={}", id);
        subclassRepository.deleteById(id);
    }

    // --- Feats ---

    @Transactional(readOnly = true)
    public List<FeatResponse> listFeats() {
        return featRepository.findAll().stream().map(this::toFeatResponse).toList();
    }

    @Transactional
    public FeatResponse createFeat(CreateFeatRequest request) {
        if (featRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Черта с таким названием уже существует");
        }
        Feat feat = Feat.builder()
                .name(request.getName())
                .description(request.getDescription())
                .prerequisites(request.getPrerequisites())
                .build();
        Feat saved = featRepository.save(feat);
        log.info("Admin: feat created — name='{}', id={}", saved.getName(), saved.getId());
        return toFeatResponse(saved);
    }

    @Transactional(readOnly = true)
    public FeatResponse getFeat(UUID id) {
        return toFeatResponse(featRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Черта не найдена")));
    }

    @Transactional
    public FeatResponse updateFeat(UUID id, CreateFeatRequest request) {
        Feat feat = featRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Черта не найдена"));
        if (!feat.getName().equals(request.getName()) && featRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Черта с таким названием уже существует");
        }
        feat.setName(request.getName());
        feat.setDescription(request.getDescription());
        feat.setPrerequisites(request.getPrerequisites());
        return toFeatResponse(featRepository.save(feat));
    }

    @Transactional
    public void deleteFeat(UUID id) {
        if (!featRepository.existsById(id)) {
            throw new ResourceNotFoundException("Черта не найдена");
        }
        log.info("Admin: feat deleted — id={}", id);
        featRepository.deleteById(id);
    }

    // --- Class Level Rewards ---

    @Transactional(readOnly = true)
    public List<ClassLevelRewardResponse> listClassLevelRewards(UUID classId) {
        if (!classRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Класс персонажа не найден");
        }
        return classLevelRewardRepository.findAllByCharacterClassId(classId).stream()
                .map(this::toClassLevelRewardResponse)
                .toList();
    }

    @Transactional
    public ClassLevelRewardResponse createClassLevelReward(UUID classId, CreateClassLevelRewardRequest request) {
        CharacterClass cc = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Класс персонажа не найден"));
        rewardResolverRegistry.validate(request.getRewardType(), request.getRewardId());
        ClassLevelReward clr = ClassLevelReward.builder()
                .characterClass(cc)
                .requiredLevel(request.getRequiredLevel())
                .rewardType(request.getRewardType())
                .rewardId(request.getRewardId())
                .isChoice(request.getIsChoice() != null ? request.getIsChoice() : true)
                .build();
        ClassLevelReward saved = classLevelRewardRepository.save(clr);
        log.info("Admin: class level reward created — classId={}, level={}, type={}, rewardId={}, id={}",
                classId, saved.getRequiredLevel(), saved.getRewardType(), saved.getRewardId(), saved.getId());
        return toClassLevelRewardResponse(saved);
    }

    @Transactional
    public void deleteClassLevelReward(UUID classId, UUID rewardEntryId) {
        ClassLevelReward clr = classLevelRewardRepository.findById(rewardEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Награда за уровень не найдена"));
        if (!clr.getCharacterClass().getId().equals(classId)) {
            throw new ResourceNotFoundException("Награда не относится к этому классу");
        }
        log.info("Admin: class level reward deleted — classId={}, rewardEntryId={}", classId, rewardEntryId);
        classLevelRewardRepository.delete(clr);
    }

    // --- Users & Teams (read-only) ---

    @Transactional(readOnly = true)
    public List<UserResponse> listAllUsers() {
        return userRepository.findAll().stream().map(userMapper::toResponse).toList();
    }

    private EquipmentSlot parseSlot(String slot) {
        try {
            return EquipmentSlot.valueOf(slot);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Некорректный слот экипировки: " + slot);
        }
    }

    private DamageType parseDamageType(String damageType) {
        if (damageType == null) return null;
        try {
            return DamageType.valueOf(damageType);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Некорректный тип урона: " + damageType);
        }
    }

    private SkillActivation parseSkillActivation(String activation) {
        if (activation == null) return null;
        try {
            return SkillActivation.valueOf(activation);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Некорректный тип активации: " + activation + ". Допустимо: PASSIVE, ACTIVE");
        }
    }

    private EffectRole parseEffectRole(String role) {
        try {
            return EffectRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Некорректная роль эффекта: " + role + ". Допустимо: BUFF, DEBUFF");
        }
    }

    private void validateDamageFields(String damageDice, String damageType) {
        if (damageDice != null && damageType == null) {
            throw new BadRequestException("Если указан damage_dice, damage_type обязателен");
        }
        if (damageType != null) {
            parseDamageType(damageType);
        }
    }

    private void validateItemTypeSkillFields(UUID skillId, String skillActivation) {
        if (skillId != null && skillActivation == null) {
            throw new BadRequestException("Если указан skillId, skillActivation обязателен");
        }
        if (skillActivation != null && skillId == null) {
            throw new BadRequestException("Если указан skillActivation, skillId обязателен");
        }
        if (skillActivation != null) {
            parseSkillActivation(skillActivation);
        }
    }

    // --- Skill Effects ---

    @Transactional(readOnly = true)
    public List<SkillEffectResponse> getSkillEffects(UUID skillId) {
        if (!skillRepository.existsById(skillId)) {
            throw new ResourceNotFoundException("Умение не найдено");
        }
        return skillEffectRepository.findAllBySkillId(skillId).stream()
                .map(this::toSkillEffectResponse).toList();
    }

    @Transactional
    public List<SkillEffectResponse> setSkillEffects(UUID skillId, SetSkillEffectsRequest request) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Умение не найдено"));

        for (SkillEffectRequest er : request.getEffects()) {
            EffectRole role = parseEffectRole(er.getEffectRole());
            BuffDebuff bd = buffDebuffRepository.findById(er.getBuffDebuffId())
                    .orElseThrow(() -> new ResourceNotFoundException("Бафф/дебафф не найден: " + er.getBuffDebuffId()));
            if (role == EffectRole.BUFF && !bd.getIsBuff()) {
                throw new UnprocessableEntityException("Роль BUFF несовместима с дебаффом '" + bd.getName() + "'");
            }
            if (role == EffectRole.DEBUFF && bd.getIsBuff()) {
                throw new UnprocessableEntityException("Роль DEBUFF несовместима с баффом '" + bd.getName() + "'");
            }
        }

        skillEffectRepository.deleteAllBySkillId(skillId);
        skillEffectRepository.flush();

        List<SkillEffect> newEffects = new ArrayList<>();
        for (SkillEffectRequest er : request.getEffects()) {
            BuffDebuff bd = buffDebuffRepository.findById(er.getBuffDebuffId()).orElseThrow();
            SkillEffect se = SkillEffect.builder()
                    .skill(skill)
                    .buffDebuff(bd)
                    .effectRole(parseEffectRole(er.getEffectRole()))
                    .chancePercent(er.getChancePercent())
                    .build();
            newEffects.add(skillEffectRepository.save(se));
        }
        log.info("Admin: skill effects updated — skillId={}, effectCount={}", skillId, newEffects.size());
        return newEffects.stream().map(this::toSkillEffectResponse).toList();
    }

    private SkillResponse toSkillResponse(Skill s) {
        List<SkillEffectResponse> effectResponses = s.getEffects() != null
                ? s.getEffects().stream().map(this::toSkillEffectResponse).toList()
                : List.of();
        return SkillResponse.builder()
                .id(s.getId()).name(s.getName()).description(s.getDescription())
                .skillType(s.getSkillType())
                .damageDice(s.getDamageDice())
                .damageBonus(s.getDamageBonus())
                .damageType(s.getDamageType() != null ? s.getDamageType().name() : null)
                .effects(effectResponses)
                .createdAt(s.getCreatedAt()).updatedAt(s.getUpdatedAt())
                .build();
    }

    private ItemTypeResponse toItemTypeResponse(ItemType it) {
        return ItemTypeResponse.builder()
                .id(it.getId()).name(it.getName()).description(it.getDescription())
                .slot(it.getSlot().name())
                .damageDice(it.getDamageDice())
                .damageBonus(it.getDamageBonus())
                .damageType(it.getDamageType() != null ? it.getDamageType().name() : null)
                .skillId(it.getSkill() != null ? it.getSkill().getId() : null)
                .skillName(it.getSkill() != null ? it.getSkill().getName() : null)
                .skillActivation(it.getSkillActivation() != null ? it.getSkillActivation().name() : null)
                .build();
    }

    private SkillEffectResponse toSkillEffectResponse(SkillEffect se) {
        BuffDebuff bd = se.getBuffDebuff();
        BuffDebuffResponse bdResp = BuffDebuffResponse.builder()
                .id(bd.getId()).name(bd.getName()).description(bd.getDescription())
                .effectType(bd.getEffectType())
                .targetStatId(bd.getTargetStat() != null ? bd.getTargetStat().getId() : null)
                .targetStatName(bd.getTargetStat() != null ? bd.getTargetStat().getName() : null)
                .modifierValue(bd.getModifierValue())
                .durationRounds(bd.getDurationRounds())
                .isBuff(bd.getIsBuff())
                .createdAt(bd.getCreatedAt())
                .build();
        return SkillEffectResponse.builder()
                .id(se.getId())
                .buffDebuff(bdResp)
                .effectRole(se.getEffectRole().name())
                .chancePercent(se.getChancePercent())
                .build();
    }

    private SubclassResponse toSubclassResponse(Subclass s) {
        return SubclassResponse.builder()
                .id(s.getId()).name(s.getName())
                .classId(s.getParentClass().getId()).className(s.getParentClass().getName())
                .description(s.getDescription()).createdAt(s.getCreatedAt()).updatedAt(s.getUpdatedAt())
                .build();
    }

    private FeatResponse toFeatResponse(Feat f) {
        return FeatResponse.builder()
                .id(f.getId()).name(f.getName()).description(f.getDescription())
                .prerequisites(f.getPrerequisites()).createdAt(f.getCreatedAt()).updatedAt(f.getUpdatedAt())
                .build();
    }

    // --- Backgrounds ---

    @Transactional(readOnly = true)
    public List<BackgroundResponse> listBackgrounds() {
        return backgroundRepository.findAll().stream().map(this::toBackgroundResponse).toList();
    }

    @Transactional
    public BackgroundResponse createBackground(CreateBackgroundRequest request) {
        if (backgroundRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Background with this name already exists");
        }
        Background bg = Background.builder()
                .name(request.getName())
                .description(request.getDescription())
                .skillProficiencyIdsJson(serializeStringList(request.getSkillProficiencyNames()))
                .grantedExtras(request.getGrantedExtras())
                .build();
        bg = backgroundRepository.save(bg);
        log.info("Admin: background created — name='{}', id={}", bg.getName(), bg.getId());
        return toBackgroundResponse(bg);
    }

    @Transactional(readOnly = true)
    public BackgroundResponse getBackground(UUID id) {
        Background bg = backgroundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Background not found"));
        return toBackgroundResponse(bg);
    }

    @Transactional
    public BackgroundResponse updateBackground(UUID id, CreateBackgroundRequest request) {
        Background bg = backgroundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Background not found"));
        bg.setName(request.getName());
        bg.setDescription(request.getDescription());
        bg.setSkillProficiencyIdsJson(serializeStringList(request.getSkillProficiencyNames()));
        bg.setGrantedExtras(request.getGrantedExtras());
        bg = backgroundRepository.save(bg);
        return toBackgroundResponse(bg);
    }

    @Transactional
    public void deleteBackground(UUID id) {
        if (!backgroundRepository.existsById(id)) {
            throw new ResourceNotFoundException("Background not found");
        }
        backgroundRepository.deleteById(id);
        log.info("Admin: background deleted — id={}", id);
    }

    // --- Spells ---

    @Transactional(readOnly = true)
    public List<SpellResponse> listSpells() {
        return spellRepository.findAll().stream().map(this::toSpellResponse).toList();
    }

    @Transactional
    public SpellResponse createSpell(CreateSpellRequest request) {
        Spell spell = Spell.builder()
                .name(request.getName())
                .level(request.getLevel())
                .school(request.getSchool())
                .description(request.getDescription())
                .availableToClassIdsJson(serializeUuidList(request.getAvailableToClassIds()))
                .build();
        spell = spellRepository.save(spell);
        log.info("Admin: spell created — name='{}', id={}", spell.getName(), spell.getId());
        return toSpellResponse(spell);
    }

    @Transactional(readOnly = true)
    public SpellResponse getSpell(UUID id) {
        Spell spell = spellRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Spell not found"));
        return toSpellResponse(spell);
    }

    @Transactional
    public SpellResponse updateSpell(UUID id, CreateSpellRequest request) {
        Spell spell = spellRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Spell not found"));
        spell.setName(request.getName());
        spell.setLevel(request.getLevel());
        spell.setSchool(request.getSchool());
        spell.setDescription(request.getDescription());
        spell.setAvailableToClassIdsJson(serializeUuidList(request.getAvailableToClassIds()));
        spell = spellRepository.save(spell);
        return toSpellResponse(spell);
    }

    @Transactional
    public void deleteSpell(UUID id) {
        if (!spellRepository.existsById(id)) {
            throw new ResourceNotFoundException("Spell not found");
        }
        spellRepository.deleteById(id);
        log.info("Admin: spell deleted — id={}", id);
    }

    // --- Proficiency Skills ---

    @Transactional(readOnly = true)
    public List<ProficiencySkillResponse> listProficiencySkills() {
        return proficiencySkillRepository.findAll().stream()
                .map(ps -> ProficiencySkillResponse.builder()
                        .id(ps.getId())
                        .name(ps.getName())
                        .governingStatId(ps.getGoverningStat().getId())
                        .governingStatName(ps.getGoverningStat().getName())
                        .build())
                .toList();
    }

    @Transactional
    public ProficiencySkillResponse createProficiencySkill(CreateProficiencySkillRequest request) {
        StatType stat = statTypeRepository.findById(request.getGoverningStatId())
                .orElseThrow(() -> new ResourceNotFoundException("Stat type not found"));
        ProficiencySkill ps = ProficiencySkill.builder()
                .name(request.getName())
                .governingStat(stat)
                .build();
        ps = proficiencySkillRepository.save(ps);
        log.info("Admin: proficiency skill created — name='{}', id={}", ps.getName(), ps.getId());
        return ProficiencySkillResponse.builder()
                .id(ps.getId())
                .name(ps.getName())
                .governingStatId(stat.getId())
                .governingStatName(stat.getName())
                .build();
    }

    @Transactional
    public void deleteProficiencySkill(UUID id) {
        if (!proficiencySkillRepository.existsById(id)) {
            throw new ResourceNotFoundException("Proficiency skill not found");
        }
        proficiencySkillRepository.deleteById(id);
        log.info("Admin: proficiency skill deleted — id={}", id);
    }

    // --- Response helpers ---

    private BackgroundResponse toBackgroundResponse(Background bg) {
        List<String> skillNames = new ArrayList<>();
        if (bg.getSkillProficiencyIdsJson() != null) {
            try {
                skillNames = objectMapper.readValue(bg.getSkillProficiencyIdsJson(),
                        new com.fasterxml.jackson.core.type.TypeReference<>() {});
            } catch (Exception ignored) {}
        }
        return BackgroundResponse.builder()
                .id(bg.getId())
                .name(bg.getName())
                .description(bg.getDescription())
                .skillProficiencyNames(skillNames)
                .grantedExtras(bg.getGrantedExtras())
                .build();
    }

    private SpellResponse toSpellResponse(Spell s) {
        List<java.util.UUID> classIds = new ArrayList<>();
        if (s.getAvailableToClassIdsJson() != null) {
            try {
                var ids = objectMapper.readValue(s.getAvailableToClassIdsJson(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
                classIds = ids.stream().map(java.util.UUID::fromString).toList();
            } catch (Exception ignored) {}
        }
        return SpellResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .level(s.getLevel())
                .school(s.getSchool())
                .description(s.getDescription())
                .availableToClassIds(classIds)
                .build();
    }

    private String serializeStringList(List<String> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return null;
        }
    }

    private String serializeUuidList(List<UUID> list) {
        if (list == null || list.isEmpty()) return null;
        try {
            List<String> strList = list.stream().map(UUID::toString).toList();
            return objectMapper.writeValueAsString(strList);
        } catch (Exception e) {
            return null;
        }
    }

    private ClassLevelRewardResponse toClassLevelRewardResponse(ClassLevelReward clr) {
        RewardDetailDto detail = rewardResolverRegistry.resolve(clr.getRewardType(), clr.getRewardId());
        return ClassLevelRewardResponse.builder()
                .id(clr.getId())
                .classId(clr.getCharacterClass().getId())
                .requiredLevel(clr.getRequiredLevel())
                .rewardType(clr.getRewardType())
                .rewardId(clr.getRewardId())
                .rewardName(detail.getName())
                .isChoice(clr.getIsChoice())
                .build();
    }
}
