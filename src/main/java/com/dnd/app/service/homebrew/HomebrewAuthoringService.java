package com.dnd.app.service.homebrew;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.ContentType;
import com.dnd.app.domain.enums.DamageType;
import com.dnd.app.domain.enums.EffectRole;
import com.dnd.app.domain.enums.EquipmentSlot;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.domain.enums.RewardType;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.SkillActivation;
import com.dnd.app.dto.request.*;
import com.dnd.app.dto.response.*;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.exception.UnprocessableEntityException;
import com.dnd.app.repository.*;
import com.dnd.app.service.reward.RewardResolverRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomebrewAuthoringService {

    private final HomebrewPackageRepository packageRepository;
    private final HomebrewContentItemRepository contentItemRepository;
    private final GmHomebrewLibraryRepository gmLibraryRepository;
    private final UserRepository userRepository;
    private final TagService tagService;
    private final HomebrewContentValidatorRegistry validatorRegistry;
    private final ItemTypeRepository itemTypeRepository;
    private final CharacterClassRepository classRepository;
    private final SkillRepository skillRepository;
    private final FeatRepository featRepository;
    private final SubclassRepository subclassRepository;
    private final BuffDebuffRepository buffDebuffRepository;
    private final SkillEffectRepository skillEffectRepository;
    private final StatTypeRepository statTypeRepository;
    private final ClassLevelRewardRepository classLevelRewardRepository;
    private final RewardResolverRegistry rewardResolverRegistry;

    @Transactional
    public HomebrewDetailResponse createPackage(CreateHomebrewRequest request, String username) {
        User gm = getGameMaster(username);
        Set<HomebrewTag> tags = tagService.findOrCreateTags(request.getTagNames());

        HomebrewPackage pkg = HomebrewPackage.builder()
                .author(gm)
                .title(request.getTitle())
                .description(request.getDescription())
                .tags(tags)
                .build();
        pkg = packageRepository.save(pkg);
        log.info("Homebrew package created: id={}, title='{}', by={}", pkg.getId(), pkg.getTitle(), username);
        return toDetailResponse(pkg);
    }

    @Transactional(readOnly = true)
    public Page<HomebrewPackageResponse> listMyPackages(String username, String status, Pageable pageable) {
        User gm = getGameMaster(username);

        Page<HomebrewPackage> packages;
        if ("DELETED".equalsIgnoreCase(status)) {
            packages = packageRepository.findAllByAuthorIdAndDeletedAtIsNotNull(gm.getId(), pageable);
        } else if (status != null) {
            HomebrewStatus homebrewStatus = parseStatus(status);
            packages = packageRepository.findAllByAuthorIdAndStatus(gm.getId(), homebrewStatus, pageable);
        } else {
            packages = packageRepository.findAllByAuthorId(gm.getId(), pageable);
        }
        return packages.map(this::toPackageResponse);
    }

    @Transactional(readOnly = true)
    public HomebrewDetailResponse getMyPackage(UUID id, String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findByIdAndAuthorId(id, gm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));
        return toDetailResponse(pkg);
    }

    @Transactional
    public HomebrewDetailResponse updatePackage(UUID id, UpdateHomebrewRequest request, String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findByIdAndAuthorId(id, gm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));

        if (pkg.getStatus() != HomebrewStatus.DRAFT) {
            throw new DuplicateResourceException("Пакет можно редактировать только в статусе черновика (DRAFT)");
        }

        if (request.getTitle() != null) {
            pkg.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            pkg.setDescription(request.getDescription());
        }
        if (request.getTagNames() != null) {
            Set<HomebrewTag> tags = tagService.findOrCreateTags(request.getTagNames());
            pkg.setTags(tags);
        }

        pkg = packageRepository.save(pkg);
        log.info("Homebrew package updated: id={}, by={}", id, username);
        return toDetailResponse(pkg);
    }

    @Transactional
    public HomebrewDetailResponse addContent(UUID packageId, AddContentRequest request, String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findByIdAndAuthorId(packageId, gm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));

        if (pkg.getStatus() != HomebrewStatus.DRAFT) {
            throw new DuplicateResourceException("Контент можно добавлять только в статусе черновика (DRAFT)");
        }

        String contentTypeStr = request.getContentType();
        if (!validatorRegistry.isKnownType(contentTypeStr)) {
            throw new DuplicateResourceException("Неизвестный тип контента: " + contentTypeStr);
        }

        validatorRegistry.validate(contentTypeStr, request.getContentId());

        UUID ownerId = validatorRegistry.getOwnerId(contentTypeStr, request.getContentId());
        if (ownerId != null && !ownerId.equals(gm.getId())) {
            throw new AccessDeniedException("Этот контент вам не принадлежит");
        }

        ContentType contentType = ContentType.valueOf(contentTypeStr);
        if (contentItemRepository.existsByHomebrewPackageIdAndContentTypeAndContentId(
                packageId, contentType, request.getContentId())) {
            throw new DuplicateResourceException("Этот контент уже есть в пакете");
        }

        HomebrewContentItem item = HomebrewContentItem.builder()
                .homebrewPackage(pkg)
                .contentType(contentType)
                .contentId(request.getContentId())
                .build();
        contentItemRepository.save(item);
        log.info("Content added to package: packageId={}, type={}, contentId={}", packageId, contentTypeStr, request.getContentId());

        return toDetailResponse(packageRepository.findById(packageId).orElseThrow());
    }

    @Transactional
    public HomebrewDetailResponse createPackageItemType(UUID packageId, CreateItemTypeRequest request, String username) {
        User gm = getRequiredGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);

        if (itemTypeRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Тип предмета с таким названием уже существует");
        }
        validateDamageFields(request.getDamageDice(), request.getDamageType());
        validateItemTypeSkillFields(request.getSkillId(), request.getSkillActivation());

        ItemType itemType = ItemType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .slot(parseSlot(request.getSlot()))
                .homebrew(pkg)
                .damageDice(request.getDamageDice())
                .damageBonus(request.getDamageBonus() != null ? request.getDamageBonus() : 0)
                .damageType(parseDamageType(request.getDamageType()))
                .build();
        if (request.getSkillId() != null) {
            Skill skill = getAllowedPackageSkill(request.getSkillId(), pkg);
            itemType.setSkill(skill);
            itemType.setSkillActivation(parseSkillActivation(request.getSkillActivation()));
        }

        ItemType saved = itemTypeRepository.save(itemType);
        attachContentItem(pkg, ContentType.ITEM_TYPE, saved.getId(), username);
        return toDetailResponse(pkg);
    }

    @Transactional
    public HomebrewDetailResponse createPackageCharacterClass(UUID packageId, CreateCharacterClassRequest request, String username) {
        User gm = getRequiredGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);

        if (classRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Класс персонажа с таким названием уже существует");
        }

        CharacterClass characterClass = CharacterClass.builder()
                .name(request.getName())
                .description(request.getDescription())
                .homebrew(pkg)
                .build();
        CharacterClass saved = classRepository.save(characterClass);
        attachContentItem(pkg, ContentType.CHARACTER_CLASS, saved.getId(), username);
        return toDetailResponse(pkg);
    }

    @Transactional
    public HomebrewClassCreationResponse createPackageCharacterClassRich(
            UUID packageId,
            CreateHomebrewClassRequest request,
            String username) {
        User gm = getRequiredGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);

        if (classRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Класс персонажа с таким названием уже существует");
        }

        CharacterClass characterClass = CharacterClass.builder()
                .name(request.getName())
                .description(request.getDescription())
                .homebrew(pkg)
                .build();
        CharacterClass savedClass = classRepository.save(characterClass);
        attachContentItem(pkg, ContentType.CHARACTER_CLASS, savedClass.getId(), username);

        Map<String, List<ContentSummaryDto>> createdContent = new LinkedHashMap<>();
        addCreatedContent(createdContent, ContentType.CHARACTER_CLASS, summarizeClass(savedClass));

        List<ClassLevelRewardResponse> rewardResponses = new ArrayList<>();
        Set<String> duplicateGuard = new HashSet<>();
        if (request.getLevels() != null) {
            for (CreateHomebrewClassRequest.LevelPlan level : request.getLevels()) {
                if (level.getRewards() == null) {
                    continue;
                }
                for (CreateHomebrewClassRequest.RewardPlan rewardPlan : level.getRewards()) {
                    RewardType rewardType = parseRewardType(rewardPlan.getRewardType());
                    UUID rewardId = resolveClassRewardId(pkg, savedClass, rewardType, rewardPlan, createdContent, username);
                    String duplicateKey = level.getLevel() + "|" + rewardType.name() + "|" + rewardId;
                    if (!duplicateGuard.add(duplicateKey)) {
                        throw new DuplicateResourceException("Дублирующаяся награда класса: " + duplicateKey);
                    }

                    ClassLevelReward reward = ClassLevelReward.builder()
                            .characterClass(savedClass)
                            .requiredLevel(level.getLevel())
                            .rewardType(rewardType.name())
                            .rewardId(rewardId)
                            .isChoice(rewardPlan.getIsChoice() != null ? rewardPlan.getIsChoice() : true)
                            .build();
                    ClassLevelReward savedReward = classLevelRewardRepository.save(reward);
                    rewardResponses.add(toClassLevelRewardResponse(savedReward));
                }
            }
        }

        HomebrewPackage refreshed = packageRepository.findById(packageId).orElseThrow();
        log.info("Homebrew class created: packageId={}, classId={}, rewards={}, author={}",
                packageId, savedClass.getId(), rewardResponses.size(), username);
        return HomebrewClassCreationResponse.builder()
                .characterClass(toCharacterClassResponse(savedClass))
                .rewards(rewardResponses)
                .createdContent(createdContent)
                .packageDetail(toDetailResponse(refreshed))
                .build();
    }

    @Transactional
    public HomebrewClassCreationResponse updatePackageCharacterClassRich(
            UUID packageId,
            UUID classId,
            CreateHomebrewClassRequest request,
            String username) {
        User gm = getRequiredGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);
        CharacterClass characterClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Класс персонажа не найден"));
        requireSamePackage(pkg, characterClass.getHomebrew(), "Класс персонажа");
        if (!characterClass.getName().equals(request.getName()) && classRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Класс персонажа с таким названием уже существует");
        }
        characterClass.setName(request.getName());
        characterClass.setDescription(request.getDescription());
        CharacterClass savedClass = classRepository.save(characterClass);
        HomebrewClassCreationResponse response = writeClassPlan(pkg, savedClass, request, username, true);
        HomebrewPackage refreshed = packageRepository.findById(packageId).orElseThrow();
        return HomebrewClassCreationResponse.builder()
                .characterClass(toCharacterClassResponse(savedClass))
                .rewards(response.getRewards())
                .createdContent(response.getCreatedContent())
                .packageDetail(toDetailResponse(refreshed))
                .build();
    }

    @Transactional
    public HomebrewClassCreationResponse createStandardCharacterClassRich(CreateHomebrewClassRequest request) {
        if (classRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Класс персонажа с таким названием уже существует");
        }
        CharacterClass characterClass = CharacterClass.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        CharacterClass savedClass = classRepository.save(characterClass);
        return writeClassPlan(null, savedClass, request, "admin", false);
    }

    @Transactional
    public HomebrewClassCreationResponse updateStandardCharacterClassRich(UUID classId, CreateHomebrewClassRequest request) {
        CharacterClass characterClass = classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Класс персонажа не найден"));
        if (characterClass.getHomebrew() != null) {
            throw new AccessDeniedException("Homebrew-класс нельзя редактировать через admin vanilla endpoint");
        }
        if (!characterClass.getName().equals(request.getName()) && classRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Класс персонажа с таким названием уже существует");
        }
        characterClass.setName(request.getName());
        characterClass.setDescription(request.getDescription());
        CharacterClass savedClass = classRepository.save(characterClass);
        return writeClassPlan(null, savedClass, request, "admin", true);
    }

    @Transactional
    public HomebrewDetailResponse createPackageSkill(UUID packageId, CreateSkillRequest request, String username) {
        User gm = getRequiredGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);

        if (skillRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Умение с таким названием уже существует");
        }
        validateDamageFields(request.getDamageDice(), request.getDamageType());

        Skill skill = Skill.builder()
                .name(request.getName())
                .description(request.getDescription())
                .skillType(request.getSkillType())
                .homebrew(pkg)
                .damageDice(request.getDamageDice())
                .damageBonus(request.getDamageBonus() != null ? request.getDamageBonus() : 0)
                .damageType(parseDamageType(request.getDamageType()))
                .build();
        Skill saved = skillRepository.save(skill);
        attachContentItem(pkg, ContentType.SKILL, saved.getId(), username);
        return toDetailResponse(pkg);
    }

    @Transactional
    public HomebrewDetailResponse createPackageFeat(UUID packageId, CreateFeatRequest request, String username) {
        User gm = getRequiredGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);

        if (featRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Черта с таким названием уже существует");
        }

        Feat feat = Feat.builder()
                .name(request.getName())
                .description(request.getDescription())
                .prerequisites(request.getPrerequisites())
                .homebrew(pkg)
                .build();
        Feat saved = featRepository.save(feat);
        attachContentItem(pkg, ContentType.FEAT, saved.getId(), username);
        return toDetailResponse(pkg);
    }

    @Transactional
    public HomebrewDetailResponse createPackageBuffDebuff(UUID packageId, CreateBuffDebuffRequest request, String username) {
        User gm = getRequiredGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);

        BuffDebuff saved = createBuffDebuffInPackage(pkg, request, username);
        log.info("Homebrew buff/debuff created: packageId={}, buffDebuffId={}, author={}",
                packageId, saved.getId(), username);
        return toDetailResponse(packageRepository.findById(packageId).orElseThrow());
    }

    @Transactional
    public void removeContent(UUID packageId, UUID contentItemId, String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findByIdAndAuthorId(packageId, gm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));

        if (pkg.getStatus() != HomebrewStatus.DRAFT) {
            throw new DuplicateResourceException("Контент можно удалять только в статусе черновика (DRAFT)");
        }

        HomebrewContentItem item = contentItemRepository.findById(contentItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Элемент контента не найден"));

        if (!item.getHomebrewPackage().getId().equals(packageId)) {
            throw new ResourceNotFoundException("Элемент контента не найден в этом пакете");
        }

        contentItemRepository.delete(item);
        log.info("Content removed from package: packageId={}, contentItemId={}", packageId, contentItemId);
    }

    @Transactional
    public HomebrewDetailResponse publish(UUID id, String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findByIdAndAuthorId(id, gm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));

        if (pkg.getStatus() != HomebrewStatus.DRAFT) {
            throw new DuplicateResourceException("Пакет можно опубликовать только из статуса черновика (DRAFT)");
        }

        long contentCount = contentItemRepository.countByHomebrewPackageId(id);
        if (contentCount == 0) {
            throw new UnprocessableEntityException("Для публикации в пакете должен быть минимум 1 элемент контента");
        }

        if (pkg.getTitle() == null || pkg.getTitle().isBlank()) {
            throw new UnprocessableEntityException("Для публикации у пакета должен быть непустой заголовок");
        }

        if (pkg.getPublishedAt() == null) {
            pkg.setPublishedAt(Instant.now());
        }
        pkg.setVersion(pkg.getVersion() + 1);
        pkg.setStatus(HomebrewStatus.PUBLISHED);
        pkg = packageRepository.save(pkg);
        log.info("Homebrew package published: id={}, version={}, by={}", id, pkg.getVersion(), username);
        return toDetailResponse(pkg);
    }

    @Transactional
    public HomebrewDetailResponse unpublish(UUID id, String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findByIdAndAuthorId(id, gm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));

        if (pkg.getStatus() != HomebrewStatus.PUBLISHED) {
            throw new DuplicateResourceException("Снять с публикации можно только опубликованные пакеты");
        }

        pkg.setStatus(HomebrewStatus.DRAFT);
        pkg = packageRepository.save(pkg);
        log.info("Homebrew package unpublished: id={}, by={}", id, username);
        return toDetailResponse(pkg);
    }

    @Transactional
    public Map<String, Object> softDelete(UUID id, String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findByIdAndAuthorId(id, gm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));

        long installCount = gmLibraryRepository.countByPackageId(id);

        pkg.setDeletedAt(Instant.now());
        pkg.setDeletedBy(gm);
        packageRepository.save(pkg);
        log.info("Homebrew package soft-deleted: id={}, installationCount={}, by={}", id, installCount, username);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Пакет удален");
        result.put("installationCount", installCount);
        return result;
    }

    private HomebrewPackage getEditablePackage(UUID packageId, User gm) {
        HomebrewPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));
        if (!pkg.getAuthor().getId().equals(gm.getId())) {
            throw new AccessDeniedException("Нельзя создавать контент в чужом homebrew-пакете");
        }
        if (pkg.isDeleted() || pkg.getStatus() != HomebrewStatus.DRAFT) {
            throw new DuplicateResourceException("Контент можно создавать только в DRAFT-пакете");
        }
        return pkg;
    }

    private void attachContentItem(HomebrewPackage pkg, ContentType contentType, UUID contentId, String username) {
        UUID packageId = pkg.getId();
        if (contentItemRepository.existsByHomebrewPackageIdAndContentTypeAndContentId(packageId, contentType, contentId)) {
            throw new DuplicateResourceException("Этот контент уже есть в пакете");
        }

        HomebrewContentItem item = HomebrewContentItem.builder()
                .homebrewPackage(pkg)
                .contentType(contentType)
                .contentId(contentId)
                .build();
        contentItemRepository.save(item);
        log.info("Homebrew authoring content created: packageId={}, contentType={}, contentId={}, author={}",
                packageId, contentType, contentId, username);
    }

    private EquipmentSlot parseSlot(String slot) {
        try {
            return EquipmentSlot.valueOf(slot);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Некорректный слот экипировки: " + slot);
        }
    }

    private DamageType parseDamageType(String damageType) {
        if (damageType == null) {
            return null;
        }
        try {
            return DamageType.valueOf(damageType);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Некорректный тип урона: " + damageType);
        }
    }

    private SkillActivation parseSkillActivation(String activation) {
        if (activation == null) {
            return null;
        }
        try {
            return SkillActivation.valueOf(activation);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Некорректный тип активации: " + activation + ". Допустимо: PASSIVE, ACTIVE");
        }
    }

    private void validateDamageFields(String damageDice, String damageType) {
        if (damageDice != null && damageType == null) {
            throw new BadRequestException("Если указан damageDice, damageType обязателен");
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

    private Skill getAllowedPackageSkill(UUID skillId, HomebrewPackage pkg) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Умение не найдено"));
        HomebrewPackage skillPackage = skill.getHomebrew();
        if (skillPackage != null && !skillPackage.getId().equals(pkg.getId())) {
            throw new AccessDeniedException("Можно привязать только vanilla-умение или умение из этого же пакета");
        }
        return skill;
    }

    private HomebrewClassCreationResponse writeClassPlan(
            HomebrewPackage pkg,
            CharacterClass characterClass,
            CreateHomebrewClassRequest request,
            String username,
            boolean replaceRewards) {
        if (replaceRewards) {
            classLevelRewardRepository.deleteAllByCharacterClassId(characterClass.getId());
            classLevelRewardRepository.flush();
        }

        Map<String, List<ContentSummaryDto>> createdContent = new LinkedHashMap<>();
        addCreatedContent(createdContent, ContentType.CHARACTER_CLASS, summarizeClass(characterClass));

        List<ClassLevelRewardResponse> rewardResponses = new ArrayList<>();
        Set<String> duplicateGuard = new HashSet<>();
        if (request.getLevels() != null) {
            for (CreateHomebrewClassRequest.LevelPlan level : request.getLevels()) {
                if (level.getRewards() == null) {
                    continue;
                }
                for (CreateHomebrewClassRequest.RewardPlan rewardPlan : level.getRewards()) {
                    RewardType rewardType = parseRewardType(rewardPlan.getRewardType());
                    UUID rewardId = resolveClassRewardId(pkg, characterClass, rewardType, rewardPlan, createdContent, username);
                    String duplicateKey = level.getLevel() + "|" + rewardType.name() + "|" + rewardId;
                    if (!duplicateGuard.add(duplicateKey)) {
                        throw new DuplicateResourceException("Дублирующаяся награда класса: " + duplicateKey);
                    }
                    ClassLevelReward reward = ClassLevelReward.builder()
                            .characterClass(characterClass)
                            .requiredLevel(level.getLevel())
                            .rewardType(rewardType.name())
                            .rewardId(rewardId)
                            .isChoice(rewardPlan.getIsChoice() != null ? rewardPlan.getIsChoice() : true)
                            .build();
                    rewardResponses.add(toClassLevelRewardResponse(classLevelRewardRepository.save(reward)));
                }
            }
        }

        return HomebrewClassCreationResponse.builder()
                .characterClass(toCharacterClassResponse(characterClass))
                .rewards(rewardResponses)
                .createdContent(createdContent)
                .packageDetail(pkg != null ? toDetailResponse(packageRepository.findById(pkg.getId()).orElseThrow()) : null)
                .build();
    }

    private RewardType parseRewardType(String rewardType) {
        try {
            return RewardType.valueOf(rewardType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Некорректный тип награды: " + rewardType);
        }
    }

    private UUID resolveClassRewardId(
            HomebrewPackage pkg,
            CharacterClass characterClass,
            RewardType rewardType,
            CreateHomebrewClassRequest.RewardPlan rewardPlan,
            Map<String, List<ContentSummaryDto>> createdContent,
            String username) {
        int sourceCount = 0;
        sourceCount += rewardPlan.getRewardId() != null ? 1 : 0;
        sourceCount += rewardPlan.getSkill() != null ? 1 : 0;
        sourceCount += rewardPlan.getFeat() != null ? 1 : 0;
        sourceCount += rewardPlan.getBuffDebuff() != null ? 1 : 0;
        sourceCount += rewardPlan.getSubclass() != null ? 1 : 0;
        if (sourceCount != 1) {
            throw new BadRequestException("Для награды должен быть указан ровно один источник: rewardId или inline-контент");
        }

        if (rewardPlan.getRewardId() != null) {
            return requireExistingRewardInPackage(pkg, characterClass, rewardType, rewardPlan.getRewardId());
        }

        return switch (rewardType) {
            case SKILL -> {
                if (rewardPlan.getSkill() == null) {
                    throw new BadRequestException("Для rewardType=SKILL ожидается поле skill");
                }
                yield createSkillInPackage(pkg, rewardPlan.getSkill(), createdContent, username).getId();
            }
            case FEAT -> {
                if (rewardPlan.getFeat() == null) {
                    throw new BadRequestException("Для rewardType=FEAT ожидается поле feat");
                }
                yield createFeatInPackage(pkg, rewardPlan.getFeat(), createdContent, username).getId();
            }
            case BUFF_DEBUFF -> {
                if (rewardPlan.getBuffDebuff() == null) {
                    throw new BadRequestException("Для rewardType=BUFF_DEBUFF ожидается поле buffDebuff");
                }
                BuffDebuff saved = createBuffDebuffInPackage(pkg, rewardPlan.getBuffDebuff(), username);
                addCreatedContent(createdContent, ContentType.BUFF_DEBUFF, summarizeBuffDebuff(saved));
                yield saved.getId();
            }
            case SUBCLASS -> {
                if (rewardPlan.getSubclass() == null) {
                    throw new BadRequestException("Для rewardType=SUBCLASS ожидается поле subclass");
                }
                yield createSubclassInPackage(pkg, characterClass, rewardPlan.getSubclass(), createdContent, username).getId();
            }
        };
    }

    private UUID requireExistingRewardInPackage(
            HomebrewPackage pkg,
            CharacterClass characterClass,
            RewardType rewardType,
            UUID rewardId) {
        switch (rewardType) {
            case SKILL -> {
                Skill skill = skillRepository.findById(rewardId)
                        .orElseThrow(() -> new ResourceNotFoundException("Умение не найдено: " + rewardId));
                requireSamePackage(pkg, skill.getHomebrew(), "Умение");
                return skill.getId();
            }
            case FEAT -> {
                Feat feat = featRepository.findById(rewardId)
                        .orElseThrow(() -> new ResourceNotFoundException("Черта не найдена: " + rewardId));
                requireSamePackage(pkg, feat.getHomebrew(), "Черта");
                return feat.getId();
            }
            case BUFF_DEBUFF -> {
                BuffDebuff buffDebuff = buffDebuffRepository.findById(rewardId)
                        .orElseThrow(() -> new ResourceNotFoundException("Бафф/дебафф не найден: " + rewardId));
                requireSamePackage(pkg, buffDebuff.getHomebrew(), "Бафф/дебафф");
                return buffDebuff.getId();
            }
            case SUBCLASS -> {
                Subclass subclass = subclassRepository.findById(rewardId)
                        .orElseThrow(() -> new ResourceNotFoundException("Подкласс не найден: " + rewardId));
                requireSamePackage(pkg, subclass.getHomebrew(), "Подкласс");
                if (subclass.getParentClass() == null || !subclass.getParentClass().getId().equals(characterClass.getId())) {
                    throw new UnprocessableEntityException("Подкласс должен относиться к создаваемому классу");
                }
                return subclass.getId();
            }
        }
        throw new BadRequestException("Некорректный тип награды: " + rewardType);
    }

    private Skill createSkillInPackage(
            HomebrewPackage pkg,
            CreateHomebrewClassRequest.InlineSkillRequest request,
            Map<String, List<ContentSummaryDto>> createdContent,
            String username) {
        if (skillRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Умение с таким названием уже существует");
        }
        validateDamageFields(request.getDamageDice(), request.getDamageType());

        Skill skill = Skill.builder()
                .name(request.getName())
                .description(request.getDescription())
                .skillType(request.getSkillType())
                .homebrew(pkg)
                .damageDice(request.getDamageDice())
                .damageBonus(request.getDamageBonus() != null ? request.getDamageBonus() : 0)
                .damageType(parseDamageType(request.getDamageType()))
                .build();
        Skill saved = skillRepository.save(skill);
        if (pkg != null) {
            attachContentItem(pkg, ContentType.SKILL, saved.getId(), username);
        }
        addCreatedContent(createdContent, ContentType.SKILL, summarizeSkill(saved));
        createSkillEffectsInPackage(pkg, saved, request.getEffects(), createdContent, username);
        return saved;
    }

    private Feat createFeatInPackage(
            HomebrewPackage pkg,
            CreateFeatRequest request,
            Map<String, List<ContentSummaryDto>> createdContent,
            String username) {
        if (featRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Черта с таким названием уже существует");
        }
        Feat feat = Feat.builder()
                .name(request.getName())
                .description(request.getDescription())
                .prerequisites(request.getPrerequisites())
                .homebrew(pkg)
                .build();
        Feat saved = featRepository.save(feat);
        if (pkg != null) {
            attachContentItem(pkg, ContentType.FEAT, saved.getId(), username);
        }
        addCreatedContent(createdContent, ContentType.FEAT, summarizeFeat(saved));
        return saved;
    }

    private Subclass createSubclassInPackage(
            HomebrewPackage pkg,
            CharacterClass characterClass,
            CreateHomebrewClassRequest.InlineSubclassRequest request,
            Map<String, List<ContentSummaryDto>> createdContent,
            String username) {
        if (subclassRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Подкласс с таким названием уже существует");
        }
        Subclass subclass = Subclass.builder()
                .name(request.getName())
                .description(request.getDescription())
                .parentClass(characterClass)
                .homebrew(pkg)
                .build();
        Subclass saved = subclassRepository.save(subclass);
        if (pkg != null) {
            attachContentItem(pkg, ContentType.SUBCLASS, saved.getId(), username);
        }
        addCreatedContent(createdContent, ContentType.SUBCLASS, summarizeSubclass(saved));
        return saved;
    }

    private BuffDebuff createBuffDebuffInPackage(HomebrewPackage pkg, CreateBuffDebuffRequest request, String username) {
        if (buffDebuffRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Бафф/дебафф с таким названием уже существует");
        }
        if ("STAT_MODIFIER".equals(request.getEffectType()) && request.getTargetStatId() == null) {
            throw new BadRequestException("Для эффекта STAT_MODIFIER нужно указать targetStatId");
        }
        BuffDebuff buffDebuff = BuffDebuff.builder()
                .name(request.getName())
                .description(request.getDescription())
                .effectType(request.getEffectType())
                .modifierValue(request.getModifierValue())
                .durationRounds(request.getDurationRounds())
                .isBuff(request.getIsBuff())
                .homebrew(pkg)
                .build();
        if (request.getTargetStatId() != null) {
            StatType statType = statTypeRepository.findById(request.getTargetStatId())
                    .orElseThrow(() -> new ResourceNotFoundException("Тип характеристики не найден: " + request.getTargetStatId()));
            requireVanillaOrSamePackage(pkg, statType.getHomebrew(), "Характеристика");
            buffDebuff.setTargetStat(statType);
        }
        BuffDebuff saved = buffDebuffRepository.save(buffDebuff);
        if (pkg != null) {
            attachContentItem(pkg, ContentType.BUFF_DEBUFF, saved.getId(), username);
        }
        return saved;
    }

    private void createSkillEffectsInPackage(
            HomebrewPackage pkg,
            Skill skill,
            List<CreateHomebrewClassRequest.InlineSkillEffectRequest> effects,
            Map<String, List<ContentSummaryDto>> createdContent,
            String username) {
        if (effects == null || effects.isEmpty()) {
            return;
        }

        Set<UUID> attachedBuffs = new HashSet<>();
        for (CreateHomebrewClassRequest.InlineSkillEffectRequest effect : effects) {
            BuffDebuff buffDebuff = resolveSkillEffectBuffDebuff(pkg, effect, createdContent, username);
            if (!attachedBuffs.add(buffDebuff.getId())) {
                throw new DuplicateResourceException("Один бафф/дебафф нельзя привязать к умению дважды: " + buffDebuff.getId());
            }
            EffectRole role = parseEffectRole(effect.getEffectRole());
            validateEffectRole(role, buffDebuff);
            SkillEffect skillEffect = SkillEffect.builder()
                    .skill(skill)
                    .buffDebuff(buffDebuff)
                    .effectRole(role)
                    .chancePercent(effect.getChancePercent())
                    .build();
            skillEffectRepository.save(skillEffect);
        }
    }

    private BuffDebuff resolveSkillEffectBuffDebuff(
            HomebrewPackage pkg,
            CreateHomebrewClassRequest.InlineSkillEffectRequest effect,
            Map<String, List<ContentSummaryDto>> createdContent,
            String username) {
        int sourceCount = 0;
        sourceCount += effect.getBuffDebuffId() != null ? 1 : 0;
        sourceCount += effect.getBuffDebuff() != null ? 1 : 0;
        if (sourceCount != 1) {
            throw new BadRequestException("Для эффекта умения нужен ровно один источник: buffDebuffId или buffDebuff");
        }
        if (effect.getBuffDebuffId() != null) {
            BuffDebuff buffDebuff = buffDebuffRepository.findById(effect.getBuffDebuffId())
                    .orElseThrow(() -> new ResourceNotFoundException("Бафф/дебафф не найден: " + effect.getBuffDebuffId()));
            requireSamePackage(pkg, buffDebuff.getHomebrew(), "Бафф/дебафф");
            return buffDebuff;
        }
        BuffDebuff saved = createBuffDebuffInPackage(pkg, effect.getBuffDebuff(), username);
        addCreatedContent(createdContent, ContentType.BUFF_DEBUFF, summarizeBuffDebuff(saved));
        return saved;
    }

    private EffectRole parseEffectRole(String role) {
        try {
            return EffectRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Некорректная роль эффекта: " + role + ". Допустимо: BUFF, DEBUFF");
        }
    }

    private void validateEffectRole(EffectRole role, BuffDebuff buffDebuff) {
        if (role == EffectRole.BUFF && !buffDebuff.getIsBuff()) {
            throw new UnprocessableEntityException("Роль BUFF несовместима с дебаффом '" + buffDebuff.getName() + "'");
        }
        if (role == EffectRole.DEBUFF && buffDebuff.getIsBuff()) {
            throw new UnprocessableEntityException("Роль DEBUFF несовместима с баффом '" + buffDebuff.getName() + "'");
        }
    }

    private void requireSamePackage(HomebrewPackage expected, HomebrewPackage actual, String contentLabel) {
        if (expected == null) {
            if (actual != null) {
                throw new AccessDeniedException(contentLabel + " должен быть стандартным, без homebrew-пакета");
            }
            return;
        }
        if (actual == null || !actual.getId().equals(expected.getId())) {
            throw new AccessDeniedException(contentLabel + " должен быть создан в этом же homebrew-пакете");
        }
    }

    private void requireVanillaOrSamePackage(HomebrewPackage expected, HomebrewPackage actual, String contentLabel) {
        if (expected == null) {
            if (actual != null) {
                throw new AccessDeniedException(contentLabel + " должен быть стандартным, без homebrew-пакета");
            }
            return;
        }
        if (actual != null && !actual.getId().equals(expected.getId())) {
            throw new AccessDeniedException(contentLabel + " должен быть vanilla или из этого же homebrew-пакета");
        }
    }

    private void addCreatedContent(
            Map<String, List<ContentSummaryDto>> createdContent,
            ContentType contentType,
            ContentSummaryDto summary) {
        createdContent.computeIfAbsent(contentType.name(), ignored -> new ArrayList<>()).add(summary);
    }

    private CharacterClassResponse toCharacterClassResponse(CharacterClass characterClass) {
        return CharacterClassResponse.builder()
                .id(characterClass.getId())
                .name(characterClass.getName())
                .description(characterClass.getDescription())
                .build();
    }

    private ClassLevelRewardResponse toClassLevelRewardResponse(ClassLevelReward reward) {
        RewardDetailDto detail = rewardResolverRegistry.resolve(reward.getRewardType(), reward.getRewardId());
        return ClassLevelRewardResponse.builder()
                .id(reward.getId())
                .classId(reward.getCharacterClass().getId())
                .requiredLevel(reward.getRequiredLevel())
                .rewardType(reward.getRewardType())
                .rewardId(reward.getRewardId())
                .rewardName(detail.getName())
                .isChoice(reward.getIsChoice())
                .build();
    }

    private ContentSummaryDto summarizeClass(CharacterClass characterClass) {
        return ContentSummaryDto.builder()
                .id(characterClass.getId())
                .name(characterClass.getName())
                .description(characterClass.getDescription())
                .build();
    }

    private ContentSummaryDto summarizeSkill(Skill skill) {
        return ContentSummaryDto.builder()
                .id(skill.getId())
                .name(skill.getName())
                .description(skill.getDescription())
                .skillType(skill.getSkillType())
                .build();
    }

    private ContentSummaryDto summarizeFeat(Feat feat) {
        return ContentSummaryDto.builder()
                .id(feat.getId())
                .name(feat.getName())
                .description(feat.getDescription())
                .prerequisites(feat.getPrerequisites())
                .build();
    }

    private ContentSummaryDto summarizeBuffDebuff(BuffDebuff buffDebuff) {
        return ContentSummaryDto.builder()
                .id(buffDebuff.getId())
                .name(buffDebuff.getName())
                .description(buffDebuff.getDescription())
                .effectType(buffDebuff.getEffectType())
                .isBuff(buffDebuff.getIsBuff())
                .build();
    }

    private ContentSummaryDto summarizeSubclass(Subclass subclass) {
        return ContentSummaryDto.builder()
                .id(subclass.getId())
                .name(subclass.getName())
                .description(subclass.getDescription())
                .classId(subclass.getParentClass() != null ? subclass.getParentClass().getId() : null)
                .className(subclass.getParentClass() != null ? subclass.getParentClass().getName() : null)
                .build();
    }

    // --- Mapping helpers ---

    HomebrewPackageResponse toPackageResponse(HomebrewPackage pkg) {
        String title = pkg.getTitle();
        if (pkg.isDeleted()) {
            title = "[УДАЛЕНО] " + title;
        }

        return HomebrewPackageResponse.builder()
                .id(pkg.getId())
                .title(title)
                .description(pkg.getDescription())
                .status(pkg.getStatus().name())
                .version(pkg.getVersion())
                .downloadCount(pkg.getDownloadCount())
                .authorUsername(pkg.getAuthor().getUsername())
                .tags(pkg.getTags().stream().map(HomebrewTag::getName).sorted().toList())
                .contentSummary(buildContentSummary(pkg.getId()))
                .publishedAt(pkg.getPublishedAt())
                .createdAt(pkg.getCreatedAt())
                .isDeleted(pkg.isDeleted())
                .build();
    }

    HomebrewDetailResponse toDetailResponse(HomebrewPackage pkg) {
        String title = pkg.getTitle();
        if (pkg.isDeleted()) {
            title = "[УДАЛЕНО] " + title;
        }

        return HomebrewDetailResponse.builder()
                .id(pkg.getId())
                .title(title)
                .description(pkg.getDescription())
                .status(pkg.getStatus().name())
                .version(pkg.getVersion())
                .downloadCount(pkg.getDownloadCount())
                .authorUsername(pkg.getAuthor().getUsername())
                .tags(pkg.getTags().stream().map(HomebrewTag::getName).sorted().toList())
                .contentSummary(buildContentSummary(pkg.getId()))
                .contentByType(buildContentByType(pkg.getId()))
                .publishedAt(pkg.getPublishedAt())
                .createdAt(pkg.getCreatedAt())
                .isDeleted(pkg.isDeleted())
                .build();
    }

    HomebrewContentSummary buildContentSummary(UUID packageId) {
        List<Object[]> counts = contentItemRepository.countByPackageGroupedByType(packageId);
        HomebrewContentSummary summary = new HomebrewContentSummary();
        for (Object[] row : counts) {
            ContentType type = (ContentType) row[0];
            int count = ((Number) row[1]).intValue();
            switch (type) {
                case ITEM_TYPE -> summary.setItemTypeCount(count);
                case CHARACTER_CLASS -> summary.setClassCount(count);
                case SKILL -> summary.setSkillCount(count);
                case FEAT -> summary.setFeatCount(count);
                case SUBCLASS -> summary.setSubclassCount(count);
                case BUFF_DEBUFF -> summary.setBuffDebuffCount(count);
            }
        }
        return summary;
    }

    private Map<String, List<ContentSummaryDto>> buildContentByType(UUID packageId) {
        List<HomebrewContentItem> items = contentItemRepository.findAllByHomebrewPackageId(packageId);
        Map<String, List<ContentSummaryDto>> result = new LinkedHashMap<>();

        Map<String, List<HomebrewContentItem>> grouped = items.stream()
                .collect(Collectors.groupingBy(i -> i.getContentType().name()));

        for (Map.Entry<String, List<HomebrewContentItem>> entry : grouped.entrySet()) {
            List<ContentSummaryDto> summaries = entry.getValue().stream()
                    .map(item -> validatorRegistry.summarize(item.getContentType().name(), item.getContentId()))
                    .toList();
            result.put(entry.getKey(), summaries);
        }
        return result;
    }

    private User getGameMaster(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (user.getRole() != Role.GAME_MASTER && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Только мастера игры могут управлять homebrew-пакетами");
        }
        return user;
    }

    private User getRequiredGameMaster(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (user.getRole() != Role.GAME_MASTER) {
            throw new AccessDeniedException("Только мастера игры могут создавать контент homebrew-пакетов");
        }
        return user;
    }

    private HomebrewStatus parseStatus(String status) {
        try {
            return HomebrewStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.dnd.app.exception.BadRequestException("Некорректный статус: " + status);
        }
    }
}
