package com.dnd.app.service.homebrew;

import com.dnd.app.domain.*;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.content.ContentSkill;
import com.dnd.app.domain.enums.ContentType;
import com.dnd.app.domain.enums.HomebrewStatus;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Класс HomebrewAuthoringService описывает сервис homebrew-логики, который проверяет и обслуживает пользовательский контент.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
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
    private final SkillRepository skillRepository;
    private final FeatRepository featRepository;
    private final BuffDebuffRepository buffDebuffRepository;
    private final StatTypeRepository statTypeRepository;
    private final BackgroundRepository backgroundRepository;
    private final ContentSkillRepository contentSkillRepository;
    private final CustomResourceTypeRepository customResourceTypeRepository;
    private final ContentCharacterClassRepository contentCharacterClassRepository;
    private final com.dnd.app.service.ContentDictionaryResolver contentDictionaryResolver;
    private final com.dnd.app.service.media.MediaUrlResolver mediaUrlResolver;

    /**
     * Создает результат операции "create package" в рамках бизнес-логики homebrew-контента.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
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

    /** Well-known название служебного пакета кастомных предметов (inscribe relic — точечная выдача). */
    private static final String SCRATCH_ITEMS_TITLE = "Мои предметы";

    /**
     * Возвращает (создавая при первом обращении) служебный DRAFT-пакет автора для кастомных предметов,
     * выдаваемых персонажам «точечной настройкой» (inscribe relic). Держит созданные на лету предметы,
     * чтобы их можно было выдать по id; в маркетплейс не публикуется.
     * @param username автор (ГМ)
     * @return деталь служебного пакета (нужен только id для ItemModal)
     */
    @Transactional
    public HomebrewDetailResponse getOrCreateScratchPackage(String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository
                .findFirstByAuthorIdAndTitleAndDeletedAtIsNull(gm.getId(), SCRATCH_ITEMS_TITLE)
                .orElseGet(() -> packageRepository.save(HomebrewPackage.builder()
                        .author(gm)
                        .title(SCRATCH_ITEMS_TITLE)
                        .description("Кастомные предметы, созданные при выдаче персонажам.")
                        .build()));
        return toDetailResponse(pkg);
    }

    /**
     * Возвращает список для операции "list my packages" в рамках бизнес-логики homebrew-контента.
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param status входящее значение status, используемое бизнес-сценарием
     * @param pageable параметры постраничной выдачи для бизнес-списка
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Возвращает результат операции "get my package" в рамках бизнес-логики homebrew-контента.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public HomebrewDetailResponse getMyPackage(UUID id, String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findByIdAndAuthorId(id, gm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));
        return toDetailResponse(pkg);
    }

    /**
     * Обновляет результат операции "update package" в рамках бизнес-логики homebrew-контента.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public HomebrewDetailResponse updatePackage(UUID id, UpdateHomebrewRequest request, String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findByIdAndAuthorId(id, gm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));

        if (!pkg.getStatus().isEditable()) {
            throw new BadRequestException("Пакет можно редактировать только в статусе DRAFT или PUBLISHED");
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

    /**
     * Добавляет результат операции "add content" в рамках бизнес-логики homebrew-контента.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public HomebrewDetailResponse addContent(UUID packageId, AddContentRequest request, String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findByIdAndAuthorId(packageId, gm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));

        if (!pkg.getStatus().isEditable()) {
            throw new BadRequestException("Контент можно добавлять только в статусе DRAFT или PUBLISHED");
        }

        String contentTypeStr = request.getContentType();
        if (!validatorRegistry.isKnownType(contentTypeStr)) {
            throw new BadRequestException("Неизвестный тип контента: " + contentTypeStr);
        }

        validatorRegistry.validate(contentTypeStr, request.getContentId());

        // Fail-closed (P0-3): владельца обязаны подтвердить. null означает либо ванильный контент,
        // либо тип без поддержки проверки владения — в обоих случаях прицеплять его к пакету нельзя,
        // иначе можно присвоить чужой/системный контент в свой homebrew-пакет.
        UUID ownerId = validatorRegistry.getOwnerId(contentTypeStr, request.getContentId());
        if (ownerId == null) {
            throw new BadRequestException(
                    "Нельзя добавить этот контент: у него нет проверяемого homebrew-владельца " +
                    "(ванильный контент или тип без поддержки проверки владения)");
        }
        if (!ownerId.equals(gm.getId())) {
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

    /**
     * Возвращает существующий контент автора заданного типа, который можно прицепить к пакету
     * (браузируемый пикер «существующее» вместо ручного ввода UUID). Показывается только собственный
     * homebrew-контент автора (удовлетворяет fail-closed проверке владения в addContent), исключая уже
     * добавленный в этот пакет; для каждого элемента даётся имя/описание и пакет-источник.
     * @param packageId идентификатор целевого пакета
     * @param contentTypeStr тип контента (ITEM_TYPE/SKILL/FEAT/BUFF_DEBUFF/…)
     * @param username имя пользователя-автора
     * @return список кандидатов на прикрепление
     */
    @Transactional(readOnly = true)
    public List<AttachableContentResponse> listAttachableContent(UUID packageId, String contentTypeStr, String username) {
        User gm = getGameMaster(username);
        packageRepository.findByIdAndAuthorId(packageId, gm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));

        if (!validatorRegistry.isKnownType(contentTypeStr)) {
            throw new BadRequestException("Неизвестный тип контента: " + contentTypeStr);
        }
        ContentType type = ContentType.valueOf(contentTypeStr);

        Set<UUID> alreadyInPackage = contentItemRepository.findContentIdsByPackageIdsAndType(Set.of(packageId), type);

        Map<UUID, AttachableContentResponse> byContentId = new LinkedHashMap<>();
        for (HomebrewContentItem item : contentItemRepository.findAttachableByAuthorAndType(gm.getId(), type)) {
            UUID cid = item.getContentId();
            if (alreadyInPackage.contains(cid) || byContentId.containsKey(cid)) {
                continue;
            }
            ContentSummaryDto summary;
            try {
                summary = validatorRegistry.summarize(type.name(), cid);
            } catch (RuntimeException e) {
                // Битая ссылка (контент удалён, а элемент-связка остался) не должна ронять весь список.
                log.warn("Skipping attachable content that failed to summarize: type={}, contentId={}", type, cid);
                continue;
            }
            byContentId.put(cid, AttachableContentResponse.builder()
                    .contentId(cid)
                    .name(summary.getName())
                    .description(summary.getDescription())
                    .sourcePackageId(item.getHomebrewPackage().getId())
                    .sourcePackageTitle(item.getHomebrewPackage().getTitle())
                    .build());
        }
        return new ArrayList<>(byContentId.values());
    }

    /**
     * Создает результат операции "create package item type" в рамках бизнес-логики homebrew-контента.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public HomebrewDetailResponse createPackageItemType(UUID packageId, CreateItemTypeRequest request, String username) {
        User gm = getRequiredGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);

        if (itemTypeRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Тип предмета с таким названием уже существует");
        }
        validateDamageFields(request.getDamageDice(), request.getDamageType(), pkg);
        validateItemTypeSkillFields(request.getSkillId(), request.getSkillActivation());

        ItemType itemType = ItemType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .slot(parseSlot(request.getSlot(), pkg))
                .homebrew(pkg)
                .createdBy(pkg.getAuthor())
                .updatedBy(pkg.getAuthor())
                .damageDice(request.getDamageDice())
                .damageBonus(request.getDamageBonus() != null ? request.getDamageBonus() : 0)
                .damageType(parseDamageType(request.getDamageType(), pkg))
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

    /**
     * Создает результат операции "create package skill" в рамках бизнес-логики homebrew-контента.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public HomebrewDetailResponse createPackageSkill(UUID packageId, CreateSkillRequest request, String username) {
        User gm = getRequiredGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);

        if (skillRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Умение с таким названием уже существует");
        }
        validateDamageFields(request.getDamageDice(), request.getDamageType(), pkg);

        Skill skill = Skill.builder()
                .name(request.getName())
                .description(request.getDescription())
                .skillType(request.getSkillType())
                .homebrew(pkg)
                .createdBy(pkg.getAuthor())
                .updatedBy(pkg.getAuthor())
                .damageDice(request.getDamageDice())
                .damageBonus(request.getDamageBonus() != null ? request.getDamageBonus() : 0)
                .damageType(parseDamageType(request.getDamageType(), pkg))
                .build();
        Skill saved = skillRepository.save(skill);
        attachContentItem(pkg, ContentType.SKILL, saved.getId(), username);
        return toDetailResponse(pkg);
    }

    /**
     * Создает результат операции "create package feat" в рамках бизнес-логики homebrew-контента.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public HomebrewDetailResponse createPackageFeat(UUID packageId, CreateFeatRequest request, String username) {
        User gm = getRequiredGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);

        if (featRepository.existsByNameRu(request.getName())) {
            throw new DuplicateResourceException("Черта с таким названием уже существует");
        }

        Feat feat = Feat.builder()
                .slug(UUID.randomUUID().toString())
                .nameRu(request.getName())
                .description(request.getDescription())
                .homebrew(pkg)
                .createdBy(pkg.getAuthor())
                .updatedBy(pkg.getAuthor())
                .build();
        Feat saved = featRepository.save(feat);
        attachContentItem(pkg, ContentType.FEAT, saved.getId(), username);
        return toDetailResponse(pkg);
    }

    /**
     * Создаёт homebrew-предысторию в пакете (P2-3). Навыки-владения резолвятся по русским названиям; свободный
     * текст доп. владений {@code grantedExtras} дописывается в описание (структурные опции — follow-up).
     * @param packageId идентификатор пакета-владельца (DRAFT)
     * @param request тело: название, (EN), описание, названия навыков-владений, доп. текст
     * @param username автор (GM, владелец пакета)
     * @return обновлённая детальная модель пакета
     */
    @Transactional
    public HomebrewDetailResponse createPackageBackground(UUID packageId, CreateBackgroundRequest request, String username) {
        User gm = getRequiredGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);

        if (backgroundRepository.existsByNameRu(request.getName())) {
            throw new DuplicateResourceException("Предыстория с таким названием уже существует");
        }

        List<ContentSkill> skills = (request.getSkillProficiencyNames() == null || request.getSkillProficiencyNames().isEmpty())
                ? new ArrayList<>()
                : new ArrayList<>(contentSkillRepository.findByNameRuIn(request.getSkillProficiencyNames()));

        String description = request.getDescription();
        if (request.getGrantedExtras() != null && !request.getGrantedExtras().isBlank()) {
            description = (description == null || description.isBlank())
                    ? request.getGrantedExtras()
                    : description + "\n\n" + request.getGrantedExtras();
        }

        Background bg = Background.builder()
                .slug(slugify(request.getName()))
                .nameRu(request.getName())
                .nameEn(request.getNameEn())
                .description(description)
                .homebrew(pkg)
                .createdBy(pkg.getAuthor())
                .updatedBy(pkg.getAuthor())
                .skillProficiencies(skills)
                .build();
        Background saved = backgroundRepository.save(bg);
        attachContentItem(pkg, ContentType.BACKGROUND, saved.getId(), username);
        return toDetailResponse(pkg);
    }

    /**
     * Создаёт homebrew-ресурс в пакете (P2-3) — тот же механизм, что Ярость/Ки (custom_resource_types).
     * Максимум задаётся числом или DSL-формулой; восстановление на коротком/длинном отдыхе валидируется
     * (none|full|formula, при formula требуется соответствующая формула). Опциональная привязка к классу.
     * @param packageId идентификатор пакета-владельца (DRAFT)
     * @param request тело ресурса
     * @param username автор (GM, владелец пакета)
     * @return обновлённая детальная модель пакета
     */
    @Transactional
    public HomebrewDetailResponse createPackageCustomResourceType(UUID packageId, CreateCustomResourceTypeRequest request, String username) {
        User gm = getRequiredGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);

        if (customResourceTypeRepository.existsByNameIgnoreCaseAndHomebrew_Id(request.getName(), pkg.getId())) {
            throw new DuplicateResourceException("Ресурс с таким названием уже существует в пакете");
        }

        String shortRecovery = normalizeRecovery(request.getShortRestRecovery(), request.getShortRestFormula(), "короткого");
        String longRecovery = normalizeRecovery(request.getLongRestRecovery(), request.getLongRestFormula(), "длинного");

        ContentCharacterClass classBound = null;
        if (request.getClassBoundId() != null) {
            classBound = contentCharacterClassRepository.findById(request.getClassBoundId())
                    .orElseThrow(() -> new BadRequestException("Класс для привязки не найден: " + request.getClassBoundId()));
        }

        CustomResourceType resource = CustomResourceType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .maxValue(request.getMaxValue())
                .maxFormula(request.getMaxFormula())
                .resetOn("none")
                .shortRestRecovery(shortRecovery)
                .shortRestFormula("formula".equals(shortRecovery) ? request.getShortRestFormula() : null)
                .longRestRecovery(longRecovery)
                .longRestFormula("formula".equals(longRecovery) ? request.getLongRestFormula() : null)
                .homebrew(pkg)
                .classBound(classBound)
                .createdBy(pkg.getAuthor())
                .updatedBy(pkg.getAuthor())
                .build();
        CustomResourceType saved = customResourceTypeRepository.save(resource);
        attachContentItem(pkg, ContentType.CUSTOM_RESOURCE, saved.getId(), username);
        return toDetailResponse(pkg);
    }

    /**
     * Нормализует правило восстановления ресурса: none|full|formula (по умолчанию none); при formula требует формулу.
     * @param recovery входное правило
     * @param formula формула (нужна при formula)
     * @param restLabel подпись отдыха для сообщения об ошибке
     * @return нормализованное правило
     */
    private String normalizeRecovery(String recovery, String formula, String restLabel) {
        String norm = (recovery == null || recovery.isBlank()) ? "none" : recovery.toLowerCase(Locale.ROOT);
        if (!Set.of("none", "full", "formula").contains(norm)) {
            throw new BadRequestException("Правило восстановления должно быть none|full|formula");
        }
        if ("formula".equals(norm) && (formula == null || formula.isBlank())) {
            throw new BadRequestException("Для восстановления по формуле (" + restLabel + " отдыха) нужна формула");
        }
        return norm;
    }

    private String slugify(String s) {
        if (s == null || s.isBlank()) {
            return "bg-" + UUID.randomUUID().toString().substring(0, 8);
        }
        String slug = s.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9а-я]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        return slug.isBlank() ? "bg-" + UUID.randomUUID().toString().substring(0, 8) : slug;
    }

    /**
     * Создает результат операции "create package buff debuff" в рамках бизнес-логики homebrew-контента.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public HomebrewDetailResponse createPackageBuffDebuff(UUID packageId, CreateBuffDebuffRequest request, String username) {
        User gm = getRequiredGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);

        BuffDebuff saved = createBuffDebuffInPackage(pkg, request, username);
        log.info("Homebrew buff/debuff created: packageId={}, buffDebuffId={}, author={}",
                packageId, saved.getId(), username);
        return toDetailResponse(packageRepository.findById(packageId).orElseThrow());
    }

    // ===================== P1-6: правка/удаление сущностей контента пакета =====================
    // Раньше у ITEM_TYPE/SKILL/FEAT/BUFF_DEBUFF был только create (write-once) — автор не мог исправить
    // опечатку или удалить свою сущность. removeContent лишь ОТВЯЗЫВАЕТ item от пакета, а тут — правка/удаление
    // самой сущности. Guard: автор пакета, статус DRAFT. DELETE — 409 при использовании (перехват FK-нарушения).

    /**
     * Обновляет homebrew-тип предмета в пакете.
     * @param packageId идентификатор пакета
     * @param itemTypeId идентификатор типа предмета
     * @param request новые данные
     * @param username автор
     * @return обновлённый пакет
     */
    @Transactional
    public HomebrewDetailResponse updatePackageItemType(UUID packageId, UUID itemTypeId, CreateItemTypeRequest request, String username) {
        HomebrewPackage pkg = getEditablePackage(packageId, getRequiredGameMaster(username));
        ItemType itemType = itemTypeRepository.findById(itemTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Тип предмета не найден"));
        requireOwnedByPackage(itemType.getHomebrew(), pkg, "Тип предмета");
        if (!itemType.getName().equals(request.getName()) && itemTypeRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Тип предмета с таким названием уже существует");
        }
        validateDamageFields(request.getDamageDice(), request.getDamageType(), pkg);
        validateItemTypeSkillFields(request.getSkillId(), request.getSkillActivation());
        itemType.setName(request.getName());
        itemType.setDescription(request.getDescription());
        itemType.setSlot(parseSlot(request.getSlot(), pkg));
        itemType.setDamageDice(request.getDamageDice());
        itemType.setDamageBonus(request.getDamageBonus() != null ? request.getDamageBonus() : 0);
        itemType.setDamageType(parseDamageType(request.getDamageType(), pkg));
        if (request.getSkillId() != null) {
            itemType.setSkill(getAllowedPackageSkill(request.getSkillId(), pkg));
            itemType.setSkillActivation(parseSkillActivation(request.getSkillActivation()));
        } else {
            itemType.setSkill(null);
            itemType.setSkillActivation(null);
        }
        itemType.setUpdatedBy(pkg.getAuthor());
        itemTypeRepository.save(itemType);
        return toDetailResponse(pkg);
    }

    /**
     * Обновляет homebrew-умение в пакете.
     */
    @Transactional
    public HomebrewDetailResponse updatePackageSkill(UUID packageId, UUID skillId, CreateSkillRequest request, String username) {
        HomebrewPackage pkg = getEditablePackage(packageId, getRequiredGameMaster(username));
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Умение не найдено"));
        requireOwnedByPackage(skill.getHomebrew(), pkg, "Умение");
        if (!skill.getName().equals(request.getName()) && skillRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Умение с таким названием уже существует");
        }
        validateDamageFields(request.getDamageDice(), request.getDamageType(), pkg);
        skill.setName(request.getName());
        skill.setDescription(request.getDescription());
        skill.setSkillType(request.getSkillType());
        skill.setDamageDice(request.getDamageDice());
        skill.setDamageBonus(request.getDamageBonus() != null ? request.getDamageBonus() : 0);
        skill.setDamageType(parseDamageType(request.getDamageType(), pkg));
        skill.setUpdatedBy(pkg.getAuthor());
        skillRepository.save(skill);
        return toDetailResponse(pkg);
    }

    /**
     * Обновляет homebrew-черту в пакете.
     */
    @Transactional
    public HomebrewDetailResponse updatePackageFeat(UUID packageId, UUID featId, CreateFeatRequest request, String username) {
        HomebrewPackage pkg = getEditablePackage(packageId, getRequiredGameMaster(username));
        Feat feat = featRepository.findById(featId)
                .orElseThrow(() -> new ResourceNotFoundException("Черта не найдена"));
        requireOwnedByPackage(feat.getHomebrew(), pkg, "Черта");
        if (!request.getName().equals(feat.getNameRu()) && featRepository.existsByNameRu(request.getName())) {
            throw new DuplicateResourceException("Черта с таким названием уже существует");
        }
        feat.setNameRu(request.getName());
        feat.setDescription(request.getDescription());
        feat.setUpdatedBy(pkg.getAuthor());
        featRepository.save(feat);
        return toDetailResponse(pkg);
    }

    /**
     * Обновляет homebrew-бафф/дебафф в пакете.
     */
    @Transactional
    public HomebrewDetailResponse updatePackageBuffDebuff(UUID packageId, UUID buffDebuffId, CreateBuffDebuffRequest request, String username) {
        HomebrewPackage pkg = getEditablePackage(packageId, getRequiredGameMaster(username));
        BuffDebuff buffDebuff = buffDebuffRepository.findById(buffDebuffId)
                .orElseThrow(() -> new ResourceNotFoundException("Бафф/дебафф не найден"));
        requireOwnedByPackage(buffDebuff.getHomebrew(), pkg, "Бафф/дебафф");
        if (!buffDebuff.getName().equals(request.getName()) && buffDebuffRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Бафф/дебафф с таким названием уже существует");
        }
        if ("STAT_MODIFIER".equals(request.getEffectType()) && request.getTargetStatId() == null) {
            throw new BadRequestException("Для эффекта STAT_MODIFIER нужно указать targetStatId");
        }
        buffDebuff.setName(request.getName());
        buffDebuff.setDescription(request.getDescription());
        buffDebuff.setEffectType(request.getEffectType());
        buffDebuff.setModifierValue(request.getModifierValue());
        buffDebuff.setDurationRounds(request.getDurationRounds());
        buffDebuff.setIsBuff(request.getIsBuff());
        if (request.getTargetStatId() != null) {
            StatType statType = statTypeRepository.findById(request.getTargetStatId())
                    .orElseThrow(() -> new ResourceNotFoundException("Тип характеристики не найден: " + request.getTargetStatId()));
            requireVanillaOrSamePackage(pkg, statType.getHomebrew(), "Характеристика");
            buffDebuff.setTargetStat(statType);
        } else {
            buffDebuff.setTargetStat(null);
        }
        buffDebuff.setUpdatedBy(pkg.getAuthor());
        buffDebuffRepository.save(buffDebuff);
        return toDetailResponse(pkg);
    }

    /**
     * Удаляет сущность контента пакета (сам объект + его content-item). 409 при использовании.
     * @param packageId пакет
     * @param contentType тип
     * @param entityId идентификатор сущности
     * @param username автор
     * @return обновлённый пакет
     */
    @Transactional
    public HomebrewDetailResponse deletePackageContentEntity(UUID packageId, String contentType, UUID entityId, String username) {
        HomebrewPackage pkg = getEditablePackage(packageId, getRequiredGameMaster(username));
        if (!validatorRegistry.isKnownType(contentType)) {
            throw new BadRequestException("Неизвестный тип контента: " + contentType);
        }
        ContentType type = ContentType.valueOf(contentType);
        Runnable deleteEntity;
        switch (type) {
            case ITEM_TYPE -> {
                ItemType e = itemTypeRepository.findById(entityId).orElseThrow(() -> new ResourceNotFoundException("Тип предмета не найден"));
                requireOwnedByPackage(e.getHomebrew(), pkg, "Тип предмета");
                deleteEntity = () -> { itemTypeRepository.delete(e); itemTypeRepository.flush(); };
            }
            case SKILL -> {
                Skill e = skillRepository.findById(entityId).orElseThrow(() -> new ResourceNotFoundException("Умение не найдено"));
                requireOwnedByPackage(e.getHomebrew(), pkg, "Умение");
                deleteEntity = () -> { skillRepository.delete(e); skillRepository.flush(); };
            }
            case FEAT -> {
                Feat e = featRepository.findById(entityId).orElseThrow(() -> new ResourceNotFoundException("Черта не найдена"));
                requireOwnedByPackage(e.getHomebrew(), pkg, "Черта");
                deleteEntity = () -> { featRepository.delete(e); featRepository.flush(); };
            }
            case BUFF_DEBUFF -> {
                BuffDebuff e = buffDebuffRepository.findById(entityId).orElseThrow(() -> new ResourceNotFoundException("Бафф/дебафф не найден"));
                requireOwnedByPackage(e.getHomebrew(), pkg, "Бафф/дебафф");
                deleteEntity = () -> { buffDebuffRepository.delete(e); buffDebuffRepository.flush(); };
            }
            default -> throw new BadRequestException("Удаление сущности этого типа не поддерживается: " + contentType);
        }
        // Сначала снимаем content-item(ы), затем удаляем сущность. FK-нарушение ⇒ 409.
        contentItemRepository.findAllByHomebrewPackageId(packageId).stream()
                .filter(ci -> ci.getContentType() == type && ci.getContentId().equals(entityId))
                .forEach(contentItemRepository::delete);
        try {
            deleteEntity.run();
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("Контент используется и не может быть удалён");
        }
        log.info("Homebrew content entity deleted: packageId={}, type={}, entityId={}, by={}", packageId, type, entityId, username);
        return toDetailResponse(packageRepository.findById(packageId).orElseThrow());
    }

    private void requireOwnedByPackage(HomebrewPackage actual, HomebrewPackage expected, String label) {
        if (actual == null || !actual.getId().equals(expected.getId())) {
            throw new AccessDeniedException(label + " не принадлежит этому пакету");
        }
    }

    /**
     * Удаляет результат операции "remove content" в рамках бизнес-логики homebrew-контента.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param contentItemId идентификатор content item, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void removeContent(UUID packageId, UUID contentItemId, String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findByIdAndAuthorId(packageId, gm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));

        if (!pkg.getStatus().isEditable()) {
            throw new BadRequestException("Контент можно удалять только в статусе DRAFT или PUBLISHED");
        }

        HomebrewContentItem item = contentItemRepository.findById(contentItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Элемент контента не найден"));

        if (!item.getHomebrewPackage().getId().equals(packageId)) {
            throw new ResourceNotFoundException("Элемент контента не найден в этом пакете");
        }

        contentItemRepository.delete(item);
        log.info("Content removed from package: packageId={}, contentItemId={}", packageId, contentItemId);
    }

    /**
     * Публикует событие операции "publish" в рамках бизнес-логики homebrew-контента.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public HomebrewDetailResponse publish(UUID id, String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findByIdAndAuthorId(id, gm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));

        if (pkg.getStatus() != HomebrewStatus.DRAFT) {
            throw new BadRequestException("Пакет можно опубликовать только из статуса черновика (DRAFT)");
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

    /**
     * Выполняет операции "unpublish" в рамках бизнес-логики homebrew-контента.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public HomebrewDetailResponse unpublish(UUID id, String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findByIdAndAuthorId(id, gm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));

        if (pkg.getStatus() != HomebrewStatus.PUBLISHED) {
            throw new BadRequestException("Снять с публикации можно только опубликованные пакеты");
        }

        pkg.setStatus(HomebrewStatus.DRAFT);
        pkg = packageRepository.save(pkg);
        log.info("Homebrew package unpublished: id={}, by={}", id, username);
        return toDetailResponse(pkg);
    }

    /**
     * Выполняет операции "soft delete" в рамках бизнес-логики homebrew-контента.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
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
        if (pkg.isDeleted() || !pkg.getStatus().isEditable()) {
            throw new BadRequestException("Контент можно создавать только в DRAFT/PUBLISHED-пакете");
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

    private EquipmentSlot parseSlot(String slot, HomebrewPackage pkg) {
        if (slot == null) {
            throw new BadRequestException("Некорректный слот экипировки: null");
        }
        return contentDictionaryResolver.resolveEquipmentSlot(slot, pkg);
    }

    private DamageType parseDamageType(String damageType, HomebrewPackage pkg) {
        return contentDictionaryResolver.resolveDamageType(damageType, pkg);
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

    private void validateDamageFields(String damageDice, String damageType, HomebrewPackage pkg) {
        if (damageDice != null && damageType == null) {
            throw new BadRequestException("Если указан damageDice, damageType обязателен");
        }
        if (damageType != null) {
            parseDamageType(damageType, pkg);
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
                .createdBy(pkg != null ? pkg.getAuthor() : null)
                .updatedBy(pkg != null ? pkg.getAuthor() : null)
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
                .coverUrl(mediaUrlResolver.resolve(
                        com.dnd.app.domain.enums.MediaOwnerType.HOMEBREW_COVER, pkg.getId(), null))
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
                .coverUrl(mediaUrlResolver.resolve(
                        com.dnd.app.domain.enums.MediaOwnerType.HOMEBREW_COVER, pkg.getId(), null))
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
                case RACE -> summary.setRaceCount(count);
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
            throw new BadRequestException("Некорректный статус: " + status);
        }
    }
}
