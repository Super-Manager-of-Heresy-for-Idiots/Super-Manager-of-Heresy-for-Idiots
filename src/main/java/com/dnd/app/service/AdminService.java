package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.EffectRole;
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
    private final CharacterRaceRepository raceRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final FeatRepository featRepository;
    private final SkillEffectRepository skillEffectRepository;
    private final BuffDebuffRepository buffDebuffRepository;
    private final BackgroundRepository backgroundRepository;
    private final SpellRepository spellRepository;
    private final ProficiencySkillRepository proficiencySkillRepository;
    private final ReferenceDataMapper refMapper;
    private final UserMapper userMapper;
    private final ContentDictionaryResolver contentDictionaryResolver;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    // --- Stat Types ---

    @Transactional(readOnly = true)
    public List<StatTypeResponse> listStatTypes() {
        return statTypeRepository.findAll().stream().map(refMapper::toStatTypeResponse).toList();
    }

    @Transactional(readOnly = true)
    public StatTypeResponse getStatType(UUID id) {
        return refMapper.toStatTypeResponse(statTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Характеристика не найдена")));
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
        log.info("Admin: item type created — name='{}', slot={}, id={}", saved.getName(), saved.getSlot().getCode(), saved.getId());
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

    // --- Feats ---

    @Transactional(readOnly = true)
    public List<FeatResponse> listFeats() {
        return featRepository.findAll().stream().map(this::toFeatResponse).toList();
    }

    @Transactional
    public FeatResponse createFeat(CreateFeatRequest request) {
        if (featRepository.existsByNameRu(request.getName())) {
            throw new DuplicateResourceException("Черта с таким названием уже существует");
        }
        Feat feat = Feat.builder()
                .slug(UUID.randomUUID().toString())
                .nameRu(request.getName())
                .description(request.getDescription())
                .build();
        Feat saved = featRepository.save(feat);
        log.info("Admin: feat created — name='{}', id={}", saved.getNameRu(), saved.getId());
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
        if (!feat.getNameRu().equals(request.getName()) && featRepository.existsByNameRu(request.getName())) {
            throw new DuplicateResourceException("Черта с таким названием уже существует");
        }
        feat.setNameRu(request.getName());
        feat.setDescription(request.getDescription());
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

    // Legacy class level-rewards authoring removed — superseded by the new content model
    // (class-builder reward groups/options/grants via ClassAuthoringService).

    // --- Users & Teams (read-only) ---

    @Transactional(readOnly = true)
    public List<UserResponse> listAllUsers() {
        return userRepository.findAll().stream().map(userMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<UserResponse> listAllUsers(org.springframework.data.domain.Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    private EquipmentSlot parseSlot(String slot) {
        if (slot == null) {
            throw new BadRequestException("Некорректный слот экипировки: null");
        }
        return contentDictionaryResolver.resolveEquipmentSlot(slot, null);
    }

    private DamageType parseDamageType(String damageType) {
        return contentDictionaryResolver.resolveDamageType(damageType, null);
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
                .damageType(s.getDamageType() != null ? s.getDamageType().getSlug() : null)
                .effects(effectResponses)
                .createdAt(s.getCreatedAt()).updatedAt(s.getUpdatedAt())
                .build();
    }

    private ItemTypeResponse toItemTypeResponse(ItemType it) {
        return ItemTypeResponse.builder()
                .id(it.getId()).name(it.getName()).description(it.getDescription())
                .slot(it.getSlot().getCode())
                .damageDice(it.getDamageDice())
                .damageBonus(it.getDamageBonus())
                .damageType(it.getDamageType() != null ? it.getDamageType().getSlug() : null)
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
                .targetStatName(bd.getTargetStat() != null ? bd.getTargetStat().getNameRu() : null)
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

    private FeatResponse toFeatResponse(Feat f) {
        return FeatResponse.builder()
                .id(f.getId()).name(f.getNameRu()).description(f.getDescription())
                .build();
    }

    // --- Backgrounds ---

    @Transactional(readOnly = true)
    public List<BackgroundResponse> listBackgrounds() {
        return backgroundRepository.findAll().stream().map(this::toBackgroundResponse).toList();
    }

    @Transactional
    public BackgroundResponse createBackground(CreateBackgroundRequest request) {
        if (backgroundRepository.existsByNameRu(request.getName())) {
            throw new DuplicateResourceException("Background with this name already exists");
        }
        Background bg = Background.builder()
                .slug(UUID.randomUUID().toString())
                .nameRu(request.getName())
                .description(request.getDescription())
                .build();
        bg = backgroundRepository.save(bg);
        log.info("Admin: background created — name='{}', id={}", bg.getNameRu(), bg.getId());
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
        bg.setNameRu(request.getName());
        bg.setDescription(request.getDescription());
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
                .slug(java.util.UUID.randomUUID().toString())
                .nameRu(request.getName())
                .level(request.getLevel())
                .description(request.getDescription())
                .build();
        spell = spellRepository.save(spell);
        log.info("Admin: spell created — name='{}', id={}", spell.getNameRu(), spell.getId());
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
        spell.setNameRu(request.getName());
        spell.setLevel(request.getLevel());
        spell.setDescription(request.getDescription());
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
                        .governingStatName(ps.getGoverningStat().getNameRu())
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
                .governingStatName(stat.getNameRu())
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
        return BackgroundResponse.builder()
                .id(bg.getId())
                .name(bg.getNameRu())
                .description(bg.getDescription())
                .skillProficiencyNames(List.of())
                .build();
    }

    private SpellResponse toSpellResponse(Spell s) {
        return SpellResponse.builder()
                .id(s.getId())
                .name(s.getNameRu())
                .level(s.getLevel())
                .school(s.getSchool() == null ? null : s.getSchool().getNameRu())
                .description(s.getDescription())
                .availableToClassIds(List.of())
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

}
