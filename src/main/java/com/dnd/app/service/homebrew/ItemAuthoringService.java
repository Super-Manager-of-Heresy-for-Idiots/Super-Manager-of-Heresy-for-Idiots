package com.dnd.app.service.homebrew;

import com.dnd.app.domain.CurrencyType;
import com.dnd.app.domain.DamageType;
import com.dnd.app.domain.HomebrewContentItem;
import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.Rarity;
import com.dnd.app.domain.content.ArmorStat;
import com.dnd.app.domain.content.DiceFormula;
import com.dnd.app.domain.content.EquipmentCategory;
import com.dnd.app.domain.content.EquipmentItem;
import com.dnd.app.domain.content.MagicItem;
import com.dnd.app.domain.content.MoneyValue;
import com.dnd.app.domain.content.WeaponStat;
import com.dnd.app.domain.enums.ContentType;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.dto.request.HomebrewItemRequest;
import com.dnd.app.dto.response.HomebrewItemResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.ArmorStatRepository;
import com.dnd.app.repository.CurrencyTypeRepository;
import com.dnd.app.repository.DiceFormulaRepository;
import com.dnd.app.repository.EquipmentCategoryRepository;
import com.dnd.app.repository.EquipmentItemRepository;
import com.dnd.app.repository.HomebrewContentItemRepository;
import com.dnd.app.repository.MagicItemRepository;
import com.dnd.app.repository.MoneyValueRepository;
import com.dnd.app.repository.WeaponStatRepository;
import com.dnd.app.service.ContentDictionaryResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Авторинг единого homebrew-предмета (P1.5 / IT-2). Снаружи «Предмет» — одна сущность; сервер маршрутизирует по
 * {@code kind}: MAGIC → magic_item, EQUIPMENT → equipment_item (+ weapon_stat/armor_stat/dice_formula/money_value),
 * TEMPLATE для новых определений запрещён. homebrew_id = packageId + авто-регистрация {@code HomebrewContentItem(ITEM)};
 * guard владелец + DRAFT; DELETE — 409 при использовании (перехват FK-нарушения).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemAuthoringService {

    /** Валидные виды снаряжения (equipment_item.kind). */
    private static final Set<String> EQUIPMENT_KINDS = Set.of("weapon", "armor", "gear", "tool");
    /** Slug'и валют, среди которых ищем «золото» для стоимости снаряжения. */
    private static final List<String> GOLD_CURRENCY_SLUGS = List.of("gp", "gold", "зм", "gold-piece");
    /** Медь за 1 золотой по умолчанию (1 gp = 100 cp), если валюта не найдена в словаре. */
    private static final BigDecimal DEFAULT_COPPER_PER_GOLD = BigDecimal.valueOf(100);

    private final MagicItemRepository magicItemRepository;
    private final EquipmentItemRepository equipmentItemRepository;
    private final WeaponStatRepository weaponStatRepository;
    private final ArmorStatRepository armorStatRepository;
    private final DiceFormulaRepository diceFormulaRepository;
    private final MoneyValueRepository moneyValueRepository;
    private final EquipmentCategoryRepository equipmentCategoryRepository;
    private final CurrencyTypeRepository currencyTypeRepository;
    private final HomebrewAccessService homebrewAccessService;
    private final HomebrewContentItemRepository contentItemRepository;
    private final ContentDictionaryResolver contentDictionaryResolver;
    private final ItemMechanicsService itemMechanicsService;

    /**
     * Создаёт предмет в пакете (маршрутизация по kind).
     * @param packageId пакет-владелец (DRAFT)
     * @param request тело (kind=MAGIC|EQUIPMENT)
     * @param username автор
     * @return созданный предмет
     */
    @Transactional
    public HomebrewItemResponse create(UUID packageId, HomebrewItemRequest request, String username) {
        HomebrewPackage pkg = editablePackage(packageId, username);
        String kind = normalizeKind(request);
        return "EQUIPMENT".equals(kind)
                ? createEquipment(pkg, request, username)
                : createMagic(pkg, request, username);
    }

    /**
     * Обновляет предмет пакета (резолв по таблицам magic_item/equipment_item; смена kind между таблицами запрещена).
     * @param packageId пакет-владелец
     * @param itemId идентификатор предмета
     * @param request тело
     * @param username автор
     * @return обновлённый предмет
     */
    @Transactional
    public HomebrewItemResponse update(UUID packageId, UUID itemId, HomebrewItemRequest request, String username) {
        HomebrewPackage pkg = editablePackage(packageId, username);
        String kind = normalizeKind(request);

        MagicItem magic = magicItemRepository.findByIdAndHomebrew_Id(itemId, packageId).orElse(null);
        if (magic != null) {
            if (!"MAGIC".equals(kind)) {
                throw new BadRequestException("Нельзя сменить вид предмета: это магический предмет");
            }
            applyMagic(magic, request, pkg);
            MagicItem saved = magicItemRepository.save(magic);
            itemMechanicsService.sync(FeatureRuleOwnerType.ITEM_MAGIC, saved.getId(), pkg, request, username);
            log.info("Homebrew magic item updated: id={}, packageId={}, by={}", itemId, packageId, username);
            return toMagicResponse(saved);
        }

        EquipmentItem equip = equipmentItemRepository.findByIdAndHomebrew_Id(itemId, packageId).orElse(null);
        if (equip != null) {
            if (!"EQUIPMENT".equals(kind)) {
                throw new BadRequestException("Нельзя сменить вид предмета: это снаряжение");
            }
            applyEquipmentCommon(equip, request, pkg);
            EquipmentItem saved = equipmentItemRepository.save(equip);
            WeaponStat weaponStat = upsertStats(saved, request, pkg);
            itemMechanicsService.sync(FeatureRuleOwnerType.ITEM_EQUIPMENT, saved.getId(), pkg, request, username);
            log.info("Homebrew equipment item updated: id={}, packageId={}, by={}", itemId, packageId, username);
            return toEquipmentResponse(saved, weaponStat, armorStatRepository.findById(saved.getId()).orElse(null));
        }

        throw new ResourceNotFoundException("Предмет не найден в этом пакете");
    }

    /**
     * Читает предмет пакета (для редактора); резолв по обеим таблицам.
     * @param packageId пакет
     * @param itemId идентификатор предмета
     * @param username пользователь
     * @return предмет для префилла
     */
    @Transactional(readOnly = true)
    public HomebrewItemResponse get(UUID packageId, UUID itemId, String username) {
        homebrewAccessService.enforceReadable(packageId, username);
        MagicItem magic = magicItemRepository.findByIdAndHomebrew_Id(itemId, packageId).orElse(null);
        if (magic != null) {
            return toMagicResponse(magic);
        }
        EquipmentItem equip = equipmentItemRepository.findByIdAndHomebrew_Id(itemId, packageId).orElse(null);
        if (equip != null) {
            return toEquipmentResponse(equip,
                    weaponStatRepository.findById(equip.getId()).orElse(null),
                    armorStatRepository.findById(equip.getId()).orElse(null));
        }
        throw new ResourceNotFoundException("Предмет не найден в этом пакете");
    }

    /**
     * Удаляет предмет пакета (резолв по обеим таблицам). 409 при использовании.
     * @param packageId пакет
     * @param itemId идентификатор предмета
     * @param username автор
     */
    @Transactional
    public void delete(UUID packageId, UUID itemId, String username) {
        editablePackage(packageId, username);

        MagicItem magic = magicItemRepository.findByIdAndHomebrew_Id(itemId, packageId).orElse(null);
        if (magic != null) {
            itemMechanicsService.clear(FeatureRuleOwnerType.ITEM_MAGIC, itemId, packageId);
            unregisterContentItem(packageId, itemId);
            deleteOrConflict(() -> {
                magicItemRepository.delete(magic);
                magicItemRepository.flush();
            });
            log.info("Homebrew magic item deleted: id={}, packageId={}, by={}", itemId, packageId, username);
            return;
        }

        EquipmentItem equip = equipmentItemRepository.findByIdAndHomebrew_Id(itemId, packageId).orElse(null);
        if (equip != null) {
            itemMechanicsService.clear(FeatureRuleOwnerType.ITEM_EQUIPMENT, itemId, packageId);
            weaponStatRepository.findById(equip.getId()).ifPresent(weaponStatRepository::delete);
            armorStatRepository.findById(equip.getId()).ifPresent(armorStatRepository::delete);
            unregisterContentItem(packageId, itemId);
            deleteOrConflict(() -> {
                equipmentItemRepository.delete(equip);
                equipmentItemRepository.flush();
            });
            log.info("Homebrew equipment item deleted: id={}, packageId={}, by={}", itemId, packageId, username);
            return;
        }

        throw new ResourceNotFoundException("Предмет не найден в этом пакете");
    }

    // ================= MAGIC =================

    private HomebrewItemResponse createMagic(HomebrewPackage pkg, HomebrewItemRequest request, String username) {
        MagicItem item = new MagicItem();
        item.setHomebrew(pkg);
        item.setCreatedBy(pkg.getAuthor());
        item.setSlug(uniqueMagicSlug(slugify(request.getName()), pkg.getId()));
        applyMagic(item, request, pkg);
        MagicItem saved = magicItemRepository.save(item);
        registerContentItem(pkg, saved.getId());
        itemMechanicsService.sync(FeatureRuleOwnerType.ITEM_MAGIC, saved.getId(), pkg, request, username);
        log.info("Homebrew magic item created: id={}, packageId={}, by={}", saved.getId(), pkg.getId(), username);
        return toMagicResponse(saved);
    }

    private void applyMagic(MagicItem item, HomebrewItemRequest request, HomebrewPackage pkg) {
        item.setUpdatedBy(pkg.getAuthor());
        item.setNameRu(request.getName());
        item.setNameEn(request.getNameEn());
        item.setDescription(request.getDescription());
        item.setVariableRarity(false);
        item.setAttunementRequired(Boolean.TRUE.equals(request.getAttunementRequired()));
        item.setAttunementRequirement(request.getAttunementRequirement());
        // Структурное ограничение настройки (HB_UX Фаза 5): слаги классов/рас нормализуются и хранятся csv.
        item.setAttunementClassSlugs(joinSlugs(request.getAttunementClassSlugs()));
        item.setAttunementRaceSlugs(joinSlugs(request.getAttunementRaceSlugs()));
        if (request.getRarity() != null && !request.getRarity().isBlank()) {
            item.setRarity(contentDictionaryResolver.resolveRarity(request.getRarity(), pkg));
        } else {
            item.setRarity(null);
        }
    }

    /** Нормализует список слагов ограничения настройки в csv (trim, lower, без пустых/дублей); null если пусто. */
    private static String joinSlugs(List<String> slugs) {
        if (slugs == null || slugs.isEmpty()) {
            return null;
        }
        List<String> clean = slugs.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
        return clean.isEmpty() ? null : String.join(",", clean);
    }

    /** Разбирает csv-слаги ограничения настройки в список (для round-trip ответа); пусто → пустой список. */
    private static List<String> splitSlugs(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private HomebrewItemResponse toMagicResponse(MagicItem item) {
        HomebrewItemResponse resp = baseResponse(item.getId(), "MAGIC", item.getNameRu(), item.getNameEn(),
                item.getDescription(), item.getHomebrew())
                .rarity(item.getRarity() != null ? item.getRarity().getSlug() : null)
                .attunementRequired(item.getAttunementRequired())
                .attunementRequirement(item.getAttunementRequirement())
                .attunementClassSlugs(splitSlugs(item.getAttunementClassSlugs()))
                .attunementRaceSlugs(splitSlugs(item.getAttunementRaceSlugs()))
                .build();
        itemMechanicsService.read(FeatureRuleOwnerType.ITEM_MAGIC, item.getId(),
                item.getHomebrew() != null ? item.getHomebrew().getId() : null, resp);
        return resp;
    }

    // ================= EQUIPMENT =================

    private HomebrewItemResponse createEquipment(HomebrewPackage pkg, HomebrewItemRequest request, String username) {
        EquipmentItem item = new EquipmentItem();
        item.setHomebrew(pkg);
        item.setCreatedBy(pkg.getAuthor());
        item.setSlug(uniqueEquipmentSlug(slugify(request.getName()), pkg.getId()));
        applyEquipmentCommon(item, request, pkg);
        EquipmentItem saved = equipmentItemRepository.save(item);
        WeaponStat weaponStat = upsertStats(saved, request, pkg);
        registerContentItem(pkg, saved.getId());
        itemMechanicsService.sync(FeatureRuleOwnerType.ITEM_EQUIPMENT, saved.getId(), pkg, request, username);
        log.info("Homebrew equipment item created: id={}, packageId={}, by={}", saved.getId(), pkg.getId(), username);
        return toEquipmentResponse(saved, weaponStat, armorStatRepository.findById(saved.getId()).orElse(null));
    }

    private void applyEquipmentCommon(EquipmentItem item, HomebrewItemRequest request, HomebrewPackage pkg) {
        item.setUpdatedBy(pkg.getAuthor());
        String kind = request.getEquipmentKind() == null ? "" : request.getEquipmentKind().toLowerCase(Locale.ROOT);
        if (!EQUIPMENT_KINDS.contains(kind)) {
            throw new BadRequestException("equipmentKind должен быть одним из: weapon, armor, gear, tool");
        }
        item.setKind(kind);
        item.setNameRu(request.getName());
        item.setNameEn(request.getNameEn());
        item.setPropertiesText(request.getDescription());
        item.setWeightLb(request.getWeightLb());
        item.setCategory(resolveCategory(request.getCategory(), pkg));
        item.setCost(buildMoneyValue(request.getCostGold()));
    }

    /**
     * Создаёт/обновляет боевой или бронированный блок для снаряжения по виду; лишний блок удаляет.
     * @param item сохранённое снаряжение (id уже сгенерирован)
     * @param request тело со stat-полями
     * @param pkg пакет (для резолвинга типа урона)
     * @return актуальный weapon-блок (или null), нужный для формирования ответа
     */
    private WeaponStat upsertStats(EquipmentItem item, HomebrewItemRequest request, HomebrewPackage pkg) {
        String kind = item.getKind();
        if ("weapon".equals(kind)) {
            armorStatRepository.findById(item.getId()).ifPresent(armorStatRepository::delete);
            WeaponStat stat = weaponStatRepository.findById(item.getId()).orElseGet(() -> {
                WeaponStat s = new WeaponStat();
                s.setEquipmentItem(item);
                return s;
            });
            stat.setDamageDiceFormula(buildDiceFormula(request));
            stat.setFlatDamage(request.getFlatDamage());
            DamageType damageType = (request.getDamageType() != null && !request.getDamageType().isBlank())
                    ? contentDictionaryResolver.resolveDamageType(request.getDamageType(), pkg) : null;
            stat.setDamageType(damageType);
            return weaponStatRepository.save(stat);
        }
        if ("armor".equals(kind)) {
            weaponStatRepository.findById(item.getId()).ifPresent(weaponStatRepository::delete);
            ArmorStat stat = armorStatRepository.findById(item.getId()).orElseGet(() -> {
                ArmorStat s = new ArmorStat();
                s.setEquipmentItem(item);
                return s;
            });
            stat.setBaseAc(request.getBaseAc());
            stat.setDexBonusAllowed(Boolean.TRUE.equals(request.getDexBonusAllowed()));
            stat.setMaxDexBonus(request.getMaxDexBonus());
            stat.setStrengthRequired(request.getStrengthRequired());
            stat.setStealthDisadvantage(Boolean.TRUE.equals(request.getStealthDisadvantage()));
            armorStatRepository.save(stat);
            return null;
        }
        // gear/tool: снять оба блока, если были
        weaponStatRepository.findById(item.getId()).ifPresent(weaponStatRepository::delete);
        armorStatRepository.findById(item.getId()).ifPresent(armorStatRepository::delete);
        return null;
    }

    private DiceFormula buildDiceFormula(HomebrewItemRequest request) {
        if (request.getDamageDiceCount() == null && request.getDamageDieSize() == null) {
            return null;
        }
        Integer count = request.getDamageDiceCount();
        Integer size = request.getDamageDieSize();
        Integer bonus = request.getDamageBonus();
        DiceFormula formula = new DiceFormula();
        formula.setDiceCount(count);
        formula.setDieSize(size);
        formula.setBonus(bonus);
        StringBuilder raw = new StringBuilder();
        raw.append(count != null ? count : 1);
        if (size != null) {
            raw.append('d').append(size);
        }
        if (bonus != null && bonus != 0) {
            raw.append(bonus > 0 ? "+" : "").append(bonus);
        }
        formula.setRawText(raw.toString());
        return diceFormulaRepository.save(formula);
    }

    private EquipmentCategory resolveCategory(String slug, HomebrewPackage pkg) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        String norm = slug.toLowerCase(Locale.ROOT);
        return equipmentCategoryRepository.findBySlugAndHomebrew_Id(norm, pkg.getId())
                .or(() -> equipmentCategoryRepository.findBySlugAndHomebrewIsNull(norm))
                .orElseThrow(() -> new BadRequestException("Неизвестная категория снаряжения: " + slug));
    }

    private MoneyValue buildMoneyValue(BigDecimal costGold) {
        if (costGold == null) {
            return null;
        }
        CurrencyType gold = resolveGoldCurrency();
        BigDecimal perGold = gold != null ? gold.getCopperValue() : DEFAULT_COPPER_PER_GOLD;
        MoneyValue mv = new MoneyValue();
        mv.setAmount(costGold);
        mv.setCurrency(gold);
        mv.setCopperValue(costGold.multiply(perGold));
        mv.setRawText(costGold.stripTrailingZeros().toPlainString() + " gp");
        return moneyValueRepository.save(mv);
    }

    private CurrencyType resolveGoldCurrency() {
        for (String slug : GOLD_CURRENCY_SLUGS) {
            CurrencyType found = currencyTypeRepository.findBySlugAndHomebrewIsNull(slug).orElse(null);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private HomebrewItemResponse toEquipmentResponse(EquipmentItem item, WeaponStat weaponStat, ArmorStat armorStat) {
        HomebrewItemResponse.HomebrewItemResponseBuilder b = baseResponse(item.getId(), "EQUIPMENT",
                item.getNameRu(), item.getNameEn(), item.getPropertiesText(), item.getHomebrew())
                .equipmentKind(item.getKind())
                .category(item.getCategory() != null ? item.getCategory().getSlug() : null)
                .costGold(item.getCost() != null ? item.getCost().getAmount() : null)
                .weightLb(item.getWeightLb());
        if (weaponStat != null) {
            DiceFormula dice = weaponStat.getDamageDiceFormula();
            b.damageDiceCount(dice != null ? dice.getDiceCount() : null)
                    .damageDieSize(dice != null ? dice.getDieSize() : null)
                    .damageBonus(dice != null ? dice.getBonus() : null)
                    .damageType(weaponStat.getDamageType() != null ? weaponStat.getDamageType().getSlug() : null)
                    .flatDamage(weaponStat.getFlatDamage());
        }
        if (armorStat != null) {
            b.baseAc(armorStat.getBaseAc())
                    .dexBonusAllowed(armorStat.getDexBonusAllowed())
                    .maxDexBonus(armorStat.getMaxDexBonus())
                    .strengthRequired(armorStat.getStrengthRequired())
                    .stealthDisadvantage(armorStat.getStealthDisadvantage());
        }
        HomebrewItemResponse resp = b.build();
        itemMechanicsService.read(FeatureRuleOwnerType.ITEM_EQUIPMENT, item.getId(),
                item.getHomebrew() != null ? item.getHomebrew().getId() : null, resp);
        return resp;
    }

    // ================= shared helpers =================

    private HomebrewItemResponse.HomebrewItemResponseBuilder baseResponse(
            UUID id, String kind, String nameRu, String nameEn, String description, HomebrewPackage pkg) {
        return HomebrewItemResponse.builder()
                .id(id)
                .kind(kind)
                .name(nameRu)
                .nameEn(nameEn)
                .description(description)
                .source(pkg != null ? "HOMEBREW" : "GLOBAL")
                .homebrewPackageId(pkg != null ? pkg.getId() : null)
                .homebrewPackageTitle(pkg != null ? pkg.getTitle() : null);
    }

    private String normalizeKind(HomebrewItemRequest request) {
        String kind = request.getKind() == null ? "MAGIC" : request.getKind().toUpperCase(Locale.ROOT);
        if ("TEMPLATE".equals(kind)) {
            throw new BadRequestException("Создание item_templates запрещено (легаси). Используйте MAGIC/EQUIPMENT.");
        }
        if (!"MAGIC".equals(kind) && !"EQUIPMENT".equals(kind)) {
            throw new BadRequestException("kind должен быть MAGIC или EQUIPMENT");
        }
        return kind;
    }

    private HomebrewPackage editablePackage(UUID packageId, String username) {
        HomebrewPackage pkg = homebrewAccessService.enforceOwner(packageId, username);
        if (pkg.isDeleted() || !pkg.getStatus().isEditable()) {
            throw new BadRequestException("Предметы можно менять только в DRAFT/PUBLISHED-пакете");
        }
        return pkg;
    }

    private void deleteOrConflict(Runnable delete) {
        try {
            delete.run();
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("Предмет используется и не может быть удалён");
        }
    }

    private void registerContentItem(HomebrewPackage pkg, UUID itemId) {
        if (!contentItemRepository.existsByHomebrewPackageIdAndContentTypeAndContentId(
                pkg.getId(), ContentType.ITEM, itemId)) {
            contentItemRepository.save(HomebrewContentItem.builder()
                    .homebrewPackage(pkg)
                    .contentType(ContentType.ITEM)
                    .contentId(itemId)
                    .build());
        }
    }

    private void unregisterContentItem(UUID packageId, UUID itemId) {
        contentItemRepository.findAllByHomebrewPackageId(packageId).stream()
                .filter(ci -> ci.getContentType() == ContentType.ITEM && ci.getContentId().equals(itemId))
                .forEach(contentItemRepository::delete);
    }

    private String uniqueMagicSlug(String base, UUID packageId) {
        String root = slugRoot(base);
        String candidate = root;
        int n = 2;
        while (magicItemRepository.existsBySlugAndHomebrew_Id(candidate, packageId)) {
            candidate = root + "-" + n++;
        }
        return candidate;
    }

    private String uniqueEquipmentSlug(String base, UUID packageId) {
        String root = slugRoot(base);
        String candidate = root;
        int n = 2;
        while (equipmentItemRepository.existsBySlugAndHomebrew_Id(candidate, packageId)) {
            candidate = root + "-" + n++;
        }
        return candidate;
    }

    private String slugRoot(String base) {
        return (base == null || base.isBlank())
                ? "item-" + UUID.randomUUID().toString().substring(0, 8) : base;
    }

    private static String slugify(String s) {
        if (s == null) {
            return "";
        }
        String slug = s.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9а-я]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        return slug.isBlank() ? "item" : slug;
    }
}
