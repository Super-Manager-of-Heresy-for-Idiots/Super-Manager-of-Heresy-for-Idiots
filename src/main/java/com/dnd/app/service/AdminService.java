package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.EquipmentSlot;
import com.dnd.app.dto.request.*;
import com.dnd.app.dto.response.*;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.mapper.ReferenceDataMapper;
import com.dnd.app.mapper.TeamMapper;
import com.dnd.app.mapper.UserMapper;
import com.dnd.app.repository.*;
import com.dnd.app.service.reward.RewardResolverRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final TeamRepository teamRepository;
    private final SkillRepository skillRepository;
    private final SubclassRepository subclassRepository;
    private final FeatRepository featRepository;
    private final ClassLevelRewardRepository classLevelRewardRepository;
    private final ReferenceDataMapper refMapper;
    private final UserMapper userMapper;
    private final TeamMapper teamMapper;
    private final RewardResolverRegistry rewardResolverRegistry;

    // --- Stat Types ---

    @Transactional(readOnly = true)
    public List<StatTypeResponse> listStatTypes() {
        return statTypeRepository.findAll().stream().map(refMapper::toStatTypeResponse).toList();
    }

    @Transactional
    public StatTypeResponse createStatType(CreateStatTypeRequest request) {
        if (statTypeRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Stat type name already exists");
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
                .orElseThrow(() -> new ResourceNotFoundException("Stat type not found")));
    }

    @Transactional
    public StatTypeResponse updateStatType(UUID id, CreateStatTypeRequest request) {
        StatType st = statTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stat type not found"));
        if (!st.getName().equals(request.getName()) && statTypeRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Stat type name already exists");
        }
        st.setName(request.getName());
        st.setDescription(request.getDescription());
        return refMapper.toStatTypeResponse(statTypeRepository.save(st));
    }

    @Transactional
    public void deleteStatType(UUID id) {
        if (!statTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Stat type not found");
        }
        log.info("Admin: stat type deleted — id={}", id);
        statTypeRepository.deleteById(id);
    }

    // --- Item Types ---

    @Transactional(readOnly = true)
    public List<ItemTypeResponse> listItemTypes() {
        return itemTypeRepository.findAll().stream().map(refMapper::toItemTypeResponse).toList();
    }

    @Transactional
    public ItemTypeResponse createItemType(CreateItemTypeRequest request) {
        if (itemTypeRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Item type name already exists");
        }
        EquipmentSlot slot = parseSlot(request.getSlot());
        ItemType it = ItemType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .slot(slot)
                .build();
        ItemType saved = itemTypeRepository.save(it);
        log.info("Admin: item type created — name='{}', slot={}, id={}", saved.getName(), saved.getSlot(), saved.getId());
        return refMapper.toItemTypeResponse(saved);
    }

    @Transactional(readOnly = true)
    public ItemTypeResponse getItemType(UUID id) {
        return refMapper.toItemTypeResponse(itemTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item type not found")));
    }

    @Transactional
    public ItemTypeResponse updateItemType(UUID id, CreateItemTypeRequest request) {
        ItemType it = itemTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item type not found"));
        if (!it.getName().equals(request.getName()) && itemTypeRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Item type name already exists");
        }
        it.setName(request.getName());
        it.setDescription(request.getDescription());
        it.setSlot(parseSlot(request.getSlot()));
        return refMapper.toItemTypeResponse(itemTypeRepository.save(it));
    }

    @Transactional
    public void deleteItemType(UUID id) {
        if (!itemTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Item type not found");
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
            throw new DuplicateResourceException("Character class name already exists");
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
                .orElseThrow(() -> new ResourceNotFoundException("Character class not found")));
    }

    @Transactional
    public CharacterClassResponse updateCharacterClass(UUID id, CreateCharacterClassRequest request) {
        CharacterClass cc = classRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Character class not found"));
        if (!cc.getName().equals(request.getName()) && classRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Character class name already exists");
        }
        cc.setName(request.getName());
        cc.setDescription(request.getDescription());
        return refMapper.toCharacterClassResponse(classRepository.save(cc));
    }

    @Transactional
    public void deleteCharacterClass(UUID id) {
        if (!classRepository.existsById(id)) {
            throw new ResourceNotFoundException("Character class not found");
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
            throw new DuplicateResourceException("Character race name already exists");
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
                .orElseThrow(() -> new ResourceNotFoundException("Character race not found")));
    }

    @Transactional
    public CharacterRaceResponse updateCharacterRace(UUID id, CreateCharacterRaceRequest request) {
        CharacterRace cr = raceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Character race not found"));
        if (!cr.getName().equals(request.getName()) && raceRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Character race name already exists");
        }
        cr.setName(request.getName());
        cr.setDescription(request.getDescription());
        return refMapper.toCharacterRaceResponse(raceRepository.save(cr));
    }

    @Transactional
    public void deleteCharacterRace(UUID id) {
        if (!raceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Character race not found");
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
            throw new DuplicateResourceException("Skill name already exists");
        }
        Skill skill = Skill.builder()
                .name(request.getName())
                .description(request.getDescription())
                .skillType(request.getSkillType())
                .build();
        Skill saved = skillRepository.save(skill);
        log.info("Admin: skill created — name='{}', type={}, id={}", saved.getName(), saved.getSkillType(), saved.getId());
        return toSkillResponse(saved);
    }

    @Transactional(readOnly = true)
    public SkillResponse getSkill(UUID id) {
        return toSkillResponse(skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found")));
    }

    @Transactional
    public SkillResponse updateSkill(UUID id, CreateSkillRequest request) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found"));
        if (!skill.getName().equals(request.getName()) && skillRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Skill name already exists");
        }
        skill.setName(request.getName());
        skill.setDescription(request.getDescription());
        skill.setSkillType(request.getSkillType());
        return toSkillResponse(skillRepository.save(skill));
    }

    @Transactional
    public void deleteSkill(UUID id) {
        if (!skillRepository.existsById(id)) {
            throw new ResourceNotFoundException("Skill not found");
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
            throw new DuplicateResourceException("Subclass name already exists");
        }
        CharacterClass parent = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Character class not found"));
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
                .orElseThrow(() -> new ResourceNotFoundException("Subclass not found")));
    }

    @Transactional
    public SubclassResponse updateSubclass(UUID id, CreateSubclassRequest request) {
        Subclass sub = subclassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subclass not found"));
        if (!sub.getName().equals(request.getName()) && subclassRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Subclass name already exists");
        }
        CharacterClass parent = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Character class not found"));
        sub.setName(request.getName());
        sub.setParentClass(parent);
        sub.setDescription(request.getDescription());
        return toSubclassResponse(subclassRepository.save(sub));
    }

    @Transactional
    public void deleteSubclass(UUID id) {
        if (!subclassRepository.existsById(id)) {
            throw new ResourceNotFoundException("Subclass not found");
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
            throw new DuplicateResourceException("Feat name already exists");
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
                .orElseThrow(() -> new ResourceNotFoundException("Feat not found")));
    }

    @Transactional
    public FeatResponse updateFeat(UUID id, CreateFeatRequest request) {
        Feat feat = featRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feat not found"));
        if (!feat.getName().equals(request.getName()) && featRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Feat name already exists");
        }
        feat.setName(request.getName());
        feat.setDescription(request.getDescription());
        feat.setPrerequisites(request.getPrerequisites());
        return toFeatResponse(featRepository.save(feat));
    }

    @Transactional
    public void deleteFeat(UUID id) {
        if (!featRepository.existsById(id)) {
            throw new ResourceNotFoundException("Feat not found");
        }
        log.info("Admin: feat deleted — id={}", id);
        featRepository.deleteById(id);
    }

    // --- Class Level Rewards ---

    @Transactional(readOnly = true)
    public List<ClassLevelRewardResponse> listClassLevelRewards(UUID classId) {
        if (!classRepository.existsById(classId)) {
            throw new ResourceNotFoundException("Character class not found");
        }
        return classLevelRewardRepository.findAllByCharacterClassId(classId).stream()
                .map(this::toClassLevelRewardResponse)
                .toList();
    }

    @Transactional
    public ClassLevelRewardResponse createClassLevelReward(UUID classId, CreateClassLevelRewardRequest request) {
        CharacterClass cc = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Character class not found"));
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
                .orElseThrow(() -> new ResourceNotFoundException("Class level reward not found"));
        if (!clr.getCharacterClass().getId().equals(classId)) {
            throw new ResourceNotFoundException("Reward does not belong to this class");
        }
        log.info("Admin: class level reward deleted — classId={}, rewardEntryId={}", classId, rewardEntryId);
        classLevelRewardRepository.delete(clr);
    }

    // --- Users & Teams (read-only) ---

    @Transactional(readOnly = true)
    public List<UserResponse> listAllUsers() {
        return userRepository.findAll().stream().map(userMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> listAllTeams() {
        return teamRepository.findAll().stream().map(teamMapper::toResponse).toList();
    }

    private EquipmentSlot parseSlot(String slot) {
        try {
            return EquipmentSlot.valueOf(slot);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid equipment slot: " + slot);
        }
    }

    private SkillResponse toSkillResponse(Skill s) {
        return SkillResponse.builder()
                .id(s.getId()).name(s.getName()).description(s.getDescription())
                .skillType(s.getSkillType()).createdAt(s.getCreatedAt()).updatedAt(s.getUpdatedAt())
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
