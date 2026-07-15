package com.dnd.app.service;

import com.dnd.app.domain.Background;
import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.Feat;
import com.dnd.app.domain.Spell;
import com.dnd.app.domain.ItemTemplate;
import com.dnd.app.domain.Rarity;
import com.dnd.app.domain.User;
import com.dnd.app.domain.content.EquipmentItem;
import com.dnd.app.domain.content.MagicItem;
import com.dnd.app.dto.content.BackgroundDetailResponse;
import com.dnd.app.dto.content.ContentLabelDto;
import com.dnd.app.dto.content.EquipmentItemDetailResponse;
import com.dnd.app.dto.content.FeatDetailResponse;
import com.dnd.app.dto.content.ItemDefinitionResponse;
import com.dnd.app.dto.content.MagicItemDetailResponse;
import com.dnd.app.dto.content.SpellDetailResponse;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.mapper.BackgroundMapper;
import com.dnd.app.mapper.EquipmentItemMapper;
import com.dnd.app.mapper.FeatMapper;
import com.dnd.app.mapper.MagicItemMapper;
import com.dnd.app.mapper.SpellMapper;
import com.dnd.app.repository.BackgroundRepository;
import com.dnd.app.repository.CampaignHomebrewRepository;
import com.dnd.app.repository.EquipmentItemRepository;
import com.dnd.app.repository.FeatRepository;
import com.dnd.app.repository.ItemTemplateRepository;
import com.dnd.app.repository.MagicItemRepository;
import com.dnd.app.repository.SpellRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.util.HomebrewOrigin;
import com.dnd.app.util.Localization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Класс ContentCatalogService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentCatalogService {

    private final FeatRepository featRepository;
    private final FeatMapper featMapper;
    private final SpellRepository spellRepository;
    private final SpellMapper spellMapper;
    private final BackgroundRepository backgroundRepository;
    private final BackgroundMapper backgroundMapper;
    private final EquipmentItemRepository equipmentItemRepository;
    private final EquipmentItemMapper equipmentItemMapper;
    private final MagicItemRepository magicItemRepository;
    private final MagicItemMapper magicItemMapper;
    private final ItemTemplateRepository itemTemplateRepository;
    private final CampaignHomebrewRepository campaignHomebrewRepository;
    private final CampaignService campaignService;
    private final UserRepository userRepository;

    // --- feats: vanilla / core only ---

    /**
     * Возвращает результат операции "get vanilla feats" в рамках бизнес-логики домена.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<FeatDetailResponse> getVanillaFeats(String lang) {
        String resolvedLang = Localization.normalize(lang);
        return featRepository.findAllByHomebrewIsNull().stream()
                .map(f -> featMapper.toDetail(f, resolvedLang))
                .toList();
    }

    /**
     * Возвращает результат операции "get vanilla feat" в рамках бизнес-логики домена.
     * @param featId идентификатор feat, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public FeatDetailResponse getVanillaFeat(UUID featId, String lang) {
        String resolvedLang = Localization.normalize(lang);
        Feat feat = featRepository.findById(featId)
                .orElseThrow(() -> new ResourceNotFoundException("Feat not found"));
        if (feat.getHomebrew() != null) {
            throw new ResourceNotFoundException("Feat not found");
        }
        return featMapper.toDetail(feat, resolvedLang);
    }

    // --- feats: campaign-aware ---

    /**
     * Возвращает результат операции "get campaign feats" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<FeatDetailResponse> getCampaignFeats(UUID campaignId, String username, String lang) {
        enforceAccess(campaignId, username);
        String resolvedLang = Localization.normalize(lang);

        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
        List<Feat> feats = new ArrayList<>(featRepository.findAllByHomebrewIsNull());
        if (!pkgIds.isEmpty()) {
            feats.addAll(featRepository.findAllByHomebrewIdIn(pkgIds));
        }
        return feats.stream().map(f -> featMapper.toDetail(f, resolvedLang)).toList();
    }

    /**
     * Возвращает результат операции "get campaign feat" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param featId идентификатор feat, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public FeatDetailResponse getCampaignFeat(UUID campaignId, UUID featId, String username, String lang) {
        enforceAccess(campaignId, username);
        String resolvedLang = Localization.normalize(lang);

        Feat feat = featRepository.findById(featId)
                .orElseThrow(() -> new ResourceNotFoundException("Feat not found"));
        enforceFeatVisibleInCampaign(campaignId, feat);
        return featMapper.toDetail(feat, resolvedLang);
    }

    // --- spells: vanilla / core only ---

    /**
     * Возвращает результат операции "get vanilla spells" в рамках бизнес-логики домена.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<SpellDetailResponse> getVanillaSpells(String lang) {
        String resolvedLang = Localization.normalize(lang);
        return spellRepository.findAllByHomebrewIsNull().stream()
                .map(s -> spellMapper.toDetail(s, resolvedLang))
                .toList();
    }

    /**
     * Возвращает результат операции "get vanilla spell" в рамках бизнес-логики домена.
     * @param spellId идентификатор spell, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public SpellDetailResponse getVanillaSpell(UUID spellId, String lang) {
        String resolvedLang = Localization.normalize(lang);
        Spell spell = spellRepository.findById(spellId)
                .orElseThrow(() -> new ResourceNotFoundException("Spell not found"));
        if (spell.getHomebrew() != null) {
            throw new ResourceNotFoundException("Spell not found");
        }
        return spellMapper.toDetail(spell, resolvedLang);
    }

    // --- spells: campaign-aware ---

    /**
     * Возвращает результат операции "get campaign spells" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<SpellDetailResponse> getCampaignSpells(UUID campaignId, String username, String lang) {
        enforceAccess(campaignId, username);
        String resolvedLang = Localization.normalize(lang);

        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
        List<Spell> spells = new ArrayList<>(spellRepository.findAllByHomebrewIsNull());
        if (!pkgIds.isEmpty()) {
            spells.addAll(spellRepository.findAllByHomebrewIdIn(pkgIds));
        }
        return spells.stream().map(s -> spellMapper.toDetail(s, resolvedLang)).toList();
    }

    /**
     * Возвращает результат операции "get campaign spell" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param spellId идентификатор spell, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public SpellDetailResponse getCampaignSpell(UUID campaignId, UUID spellId, String username, String lang) {
        enforceAccess(campaignId, username);
        String resolvedLang = Localization.normalize(lang);

        Spell spell = spellRepository.findById(spellId)
                .orElseThrow(() -> new ResourceNotFoundException("Spell not found"));
        enforceSpellVisibleInCampaign(campaignId, spell);
        return spellMapper.toDetail(spell, resolvedLang);
    }

    // --- backgrounds: vanilla / core only ---

    /**
     * Возвращает результат операции "get vanilla backgrounds" в рамках бизнес-логики домена.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<BackgroundDetailResponse> getVanillaBackgrounds(String lang) {
        String resolvedLang = Localization.normalize(lang);
        return backgroundRepository.findAllByHomebrewIsNull().stream()
                .map(b -> backgroundMapper.toDetail(b, resolvedLang))
                .toList();
    }

    /**
     * Возвращает результат операции "get vanilla background" в рамках бизнес-логики домена.
     * @param backgroundId идентификатор background, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public BackgroundDetailResponse getVanillaBackground(UUID backgroundId, String lang) {
        String resolvedLang = Localization.normalize(lang);
        Background background = backgroundRepository.findById(backgroundId)
                .orElseThrow(() -> new ResourceNotFoundException("Background not found"));
        if (background.getHomebrew() != null) {
            throw new ResourceNotFoundException("Background not found");
        }
        return backgroundMapper.toDetail(background, resolvedLang);
    }

    // --- backgrounds: campaign-aware ---

    /**
     * Возвращает результат операции "get campaign backgrounds" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<BackgroundDetailResponse> getCampaignBackgrounds(UUID campaignId, String username, String lang) {
        enforceAccess(campaignId, username);
        String resolvedLang = Localization.normalize(lang);

        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
        List<Background> backgrounds = new ArrayList<>(backgroundRepository.findAllByHomebrewIsNull());
        if (!pkgIds.isEmpty()) {
            backgrounds.addAll(backgroundRepository.findAllByHomebrewIdIn(pkgIds));
        }
        return backgrounds.stream().map(b -> backgroundMapper.toDetail(b, resolvedLang)).toList();
    }

    /**
     * Возвращает результат операции "get campaign background" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param backgroundId идентификатор background, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public BackgroundDetailResponse getCampaignBackground(UUID campaignId, UUID backgroundId, String username, String lang) {
        enforceAccess(campaignId, username);
        String resolvedLang = Localization.normalize(lang);

        Background background = backgroundRepository.findById(backgroundId)
                .orElseThrow(() -> new ResourceNotFoundException("Background not found"));
        enforceBackgroundVisibleInCampaign(campaignId, background);
        return backgroundMapper.toDetail(background, resolvedLang);
    }

    // --- equipment items: vanilla / core only ---

    /**
     * Возвращает результат операции "get vanilla equipment items" в рамках бизнес-логики домена.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<EquipmentItemDetailResponse> getVanillaEquipmentItems(String lang) {
        String resolvedLang = Localization.normalize(lang);
        return equipmentItemRepository.findAllByHomebrewIsNull().stream()
                .map(e -> equipmentItemMapper.toDetail(e, resolvedLang))
                .toList();
    }

    /**
     * Возвращает результат операции "get vanilla equipment item" в рамках бизнес-логики домена.
     * @param equipmentItemId идентификатор equipment item, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public EquipmentItemDetailResponse getVanillaEquipmentItem(UUID equipmentItemId, String lang) {
        String resolvedLang = Localization.normalize(lang);
        EquipmentItem item = equipmentItemRepository.findById(equipmentItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment item not found"));
        if (item.getHomebrew() != null) {
            throw new ResourceNotFoundException("Equipment item not found");
        }
        return equipmentItemMapper.toDetail(item, resolvedLang);
    }

    // --- equipment items: campaign-aware ---

    /**
     * Возвращает результат операции "get campaign equipment items" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<EquipmentItemDetailResponse> getCampaignEquipmentItems(UUID campaignId, String username, String lang) {
        enforceAccess(campaignId, username);
        String resolvedLang = Localization.normalize(lang);

        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
        List<EquipmentItem> items = new ArrayList<>(equipmentItemRepository.findAllByHomebrewIsNull());
        if (!pkgIds.isEmpty()) {
            items.addAll(equipmentItemRepository.findAllByHomebrewIdIn(pkgIds));
        }
        return items.stream().map(e -> equipmentItemMapper.toDetail(e, resolvedLang)).toList();
    }

    /**
     * Возвращает результат операции "get campaign equipment item" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param equipmentItemId идентификатор equipment item, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public EquipmentItemDetailResponse getCampaignEquipmentItem(UUID campaignId, UUID equipmentItemId, String username, String lang) {
        enforceAccess(campaignId, username);
        String resolvedLang = Localization.normalize(lang);

        EquipmentItem item = equipmentItemRepository.findById(equipmentItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipment item not found"));
        enforceEquipmentItemVisibleInCampaign(campaignId, item);
        return equipmentItemMapper.toDetail(item, resolvedLang);
    }

    // --- magic items: vanilla / core only ---

    /**
     * Возвращает результат операции "get vanilla magic items" в рамках бизнес-логики домена.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<MagicItemDetailResponse> getVanillaMagicItems(String lang) {
        String resolvedLang = Localization.normalize(lang);
        return magicItemRepository.findAllByHomebrewIsNull().stream()
                .map(m -> magicItemMapper.toDetail(m, resolvedLang))
                .toList();
    }

    /**
     * Возвращает результат операции "get vanilla magic item" в рамках бизнес-логики домена.
     * @param magicItemId идентификатор magic item, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public MagicItemDetailResponse getVanillaMagicItem(UUID magicItemId, String lang) {
        String resolvedLang = Localization.normalize(lang);
        MagicItem item = magicItemRepository.findById(magicItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Magic item not found"));
        if (item.getHomebrew() != null) {
            throw new ResourceNotFoundException("Magic item not found");
        }
        return magicItemMapper.toDetail(item, resolvedLang);
    }

    // --- magic items: campaign-aware ---

    /**
     * Возвращает результат операции "get campaign magic items" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<MagicItemDetailResponse> getCampaignMagicItems(UUID campaignId, String username, String lang) {
        enforceAccess(campaignId, username);
        String resolvedLang = Localization.normalize(lang);

        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
        List<MagicItem> items = new ArrayList<>(magicItemRepository.findAllByHomebrewIsNull());
        if (!pkgIds.isEmpty()) {
            items.addAll(magicItemRepository.findAllByHomebrewIdIn(pkgIds));
        }
        return items.stream().map(m -> magicItemMapper.toDetail(m, resolvedLang)).toList();
    }

    /**
     * Возвращает результат операции "get campaign magic item" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param magicItemId идентификатор magic item, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public MagicItemDetailResponse getCampaignMagicItem(UUID campaignId, UUID magicItemId, String username, String lang) {
        enforceAccess(campaignId, username);
        String resolvedLang = Localization.normalize(lang);

        MagicItem item = magicItemRepository.findById(magicItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Magic item not found"));
        enforceMagicItemVisibleInCampaign(campaignId, item);
        return magicItemMapper.toDetail(item, resolvedLang);
    }

    // --- unified item definitions (IT-1): equipment + magic + legacy template ---

    /**
     * Возвращает единый ванильный каталог «Предметов» (IT-1): снаряжение + магические + легаси-шаблоны,
     * сведённые к {@link ItemDefinitionResponse} с дискриминатором kind. Только core-контент (homebrew IS NULL).
     * @param lang язык локализации меток
     * @return список унифицированных определений предметов
     */
    @Transactional(readOnly = true)
    public List<ItemDefinitionResponse> getVanillaItems(String lang) {
        String resolvedLang = Localization.normalize(lang);
        List<ItemDefinitionResponse> out = new ArrayList<>();
        equipmentItemRepository.findAllByHomebrewIsNull()
                .forEach(e -> out.add(ItemDefinitionResponse.fromEquipment(equipmentItemMapper.toDetail(e, resolvedLang))));
        magicItemRepository.findAllByHomebrewIsNull()
                .forEach(m -> out.add(ItemDefinitionResponse.fromMagic(magicItemMapper.toDetail(m, resolvedLang))));
        itemTemplateRepository.findByHomebrewIsNull()
                .forEach(t -> out.add(toTemplateDefinition(t, resolvedLang)));
        return out;
    }

    /**
     * Возвращает единое ванильное определение предмета по id, резолвя его по трём таблицам
     * (magic_item / equipment_item / item_templates). Homebrew-строки при этом трактуются как «не найдено».
     * @param itemId идентификатор предмета в любой из трёх таблиц
     * @param lang язык локализации меток
     * @return унифицированное определение предмета
     */
    @Transactional(readOnly = true)
    public ItemDefinitionResponse getVanillaItem(UUID itemId, String lang) {
        String resolvedLang = Localization.normalize(lang);
        MagicItem magic = magicItemRepository.findById(itemId).orElse(null);
        if (magic != null) {
            if (magic.getHomebrew() != null) {
                throw new ResourceNotFoundException("Item not found");
            }
            return ItemDefinitionResponse.fromMagic(magicItemMapper.toDetail(magic, resolvedLang));
        }
        EquipmentItem equip = equipmentItemRepository.findById(itemId).orElse(null);
        if (equip != null) {
            if (equip.getHomebrew() != null) {
                throw new ResourceNotFoundException("Item not found");
            }
            return ItemDefinitionResponse.fromEquipment(equipmentItemMapper.toDetail(equip, resolvedLang));
        }
        ItemTemplate template = itemTemplateRepository.findById(itemId).orElse(null);
        if (template != null) {
            if (template.getHomebrew() != null) {
                throw new ResourceNotFoundException("Item not found");
            }
            return toTemplateDefinition(template, resolvedLang);
        }
        throw new ResourceNotFoundException("Item not found");
    }

    /**
     * Возвращает единый каталог «Предметов» для кампании (IT-1): ваниль + активные homebrew-пакеты кампании,
     * по всем трём таблицам, сведённые к {@link ItemDefinitionResponse}.
     * @param campaignId идентификатор кампании
     * @param username пользователь (проверка доступа к кампании)
     * @param lang язык локализации меток
     * @return список унифицированных определений предметов, видимых в кампании
     */
    @Transactional(readOnly = true)
    public List<ItemDefinitionResponse> getCampaignItems(UUID campaignId, String username, String lang) {
        enforceAccess(campaignId, username);
        String resolvedLang = Localization.normalize(lang);
        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);

        List<ItemDefinitionResponse> out = new ArrayList<>();

        List<EquipmentItem> equipment = new ArrayList<>(equipmentItemRepository.findAllByHomebrewIsNull());
        List<MagicItem> magic = new ArrayList<>(magicItemRepository.findAllByHomebrewIsNull());
        List<ItemTemplate> templates = new ArrayList<>(itemTemplateRepository.findByHomebrewIsNull());
        if (!pkgIds.isEmpty()) {
            equipment.addAll(equipmentItemRepository.findAllByHomebrewIdIn(pkgIds));
            magic.addAll(magicItemRepository.findAllByHomebrewIdIn(pkgIds));
            templates.addAll(itemTemplateRepository.findByHomebrewIdIn(new ArrayList<>(pkgIds)));
        }
        equipment.forEach(e -> out.add(ItemDefinitionResponse.fromEquipment(equipmentItemMapper.toDetail(e, resolvedLang))));
        magic.forEach(m -> out.add(ItemDefinitionResponse.fromMagic(magicItemMapper.toDetail(m, resolvedLang))));
        templates.forEach(t -> out.add(toTemplateDefinition(t, resolvedLang)));
        return out;
    }

    /**
     * Возвращает единое определение предмета в контексте кампании: резолв по трём таблицам + проверка,
     * что предмет видим в кампании (ваниль всегда; homebrew — только из активных пакетов кампании).
     * @param campaignId идентификатор кампании
     * @param itemId идентификатор предмета в любой из трёх таблиц
     * @param username пользователь (проверка доступа)
     * @param lang язык локализации меток
     * @return унифицированное определение предмета
     */
    @Transactional(readOnly = true)
    public ItemDefinitionResponse getCampaignItem(UUID campaignId, UUID itemId, String username, String lang) {
        enforceAccess(campaignId, username);
        String resolvedLang = Localization.normalize(lang);

        MagicItem magic = magicItemRepository.findById(itemId).orElse(null);
        if (magic != null) {
            enforceMagicItemVisibleInCampaign(campaignId, magic);
            return ItemDefinitionResponse.fromMagic(magicItemMapper.toDetail(magic, resolvedLang));
        }
        EquipmentItem equip = equipmentItemRepository.findById(itemId).orElse(null);
        if (equip != null) {
            enforceEquipmentItemVisibleInCampaign(campaignId, equip);
            return ItemDefinitionResponse.fromEquipment(equipmentItemMapper.toDetail(equip, resolvedLang));
        }
        ItemTemplate template = itemTemplateRepository.findById(itemId).orElse(null);
        if (template != null) {
            enforceTemplateVisibleInCampaign(campaignId, template);
            return toTemplateDefinition(template, resolvedLang);
        }
        throw new ResourceNotFoundException("Item not found");
    }

    /**
     * Маппит легаси-шаблон предмета в единый {@link ItemDefinitionResponse} (kind=TEMPLATE).
     * Стоимость подставляется из priceGold (сырое золото), тип — из name у {@link com.dnd.app.domain.ItemType}.
     * @param t сущность шаблона предмета
     * @param lang язык локализации редкости
     * @return унифицированное определение предмета вида TEMPLATE
     */
    private ItemDefinitionResponse toTemplateDefinition(ItemTemplate t, String lang) {
        ContentLabelDto type = t.getItemType() == null ? null : ContentLabelDto.builder()
                .id(t.getItemType().getId())
                .name(t.getItemType().getName())
                .build();
        ContentLabelDto rarity = rarityLabel(t.getRarity(), lang);
        ItemDefinitionResponse.CostDto cost = t.getPriceGold() == null ? null : ItemDefinitionResponse.CostDto.builder()
                .amount(t.getPriceGold())
                .build();
        return ItemDefinitionResponse.builder()
                .id(t.getId())
                .kind("TEMPLATE")
                .name(t.getName())
                .description(t.getDescription())
                .type(type)
                .rarity(rarity)
                .cost(cost)
                .attunementRequired(t.getAttunementRequired())
                .packageId(HomebrewOrigin.id(t.getHomebrew()))
                .source(HomebrewOrigin.source(t.getHomebrew()))
                .homebrewTitle(HomebrewOrigin.title(t.getHomebrew()))
                .build();
    }

    private ContentLabelDto rarityLabel(Rarity r, String lang) {
        if (r == null) {
            return null;
        }
        return ContentLabelDto.builder()
                .id(r.getId())
                .slug(r.getSlug())
                .name(Localization.pick(lang, r.getNameRu(), r.getNameEn(),
                        r.getNameEn() != null ? r.getNameEn() : r.getNameRu()))
                .nameRu(r.getNameRu())
                .nameEn(r.getNameEn())
                .build();
    }

    private void enforceTemplateVisibleInCampaign(UUID campaignId, ItemTemplate item) {
        if (item.getHomebrew() == null) {
            return; // core content always visible
        }
        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
        if (!pkgIds.contains(item.getHomebrew().getId())) {
            throw new ResourceNotFoundException("Item not found");
        }
    }

    // --- access helpers ---

    private void enforceMagicItemVisibleInCampaign(UUID campaignId, MagicItem item) {
        if (item.getHomebrew() == null) {
            return; // core content always visible
        }
        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
        if (!pkgIds.contains(item.getHomebrew().getId())) {
            throw new ResourceNotFoundException("Magic item not found");
        }
    }

    private void enforceEquipmentItemVisibleInCampaign(UUID campaignId, EquipmentItem item) {
        if (item.getHomebrew() == null) {
            return; // core content always visible
        }
        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
        if (!pkgIds.contains(item.getHomebrew().getId())) {
            throw new ResourceNotFoundException("Equipment item not found");
        }
    }

    private void enforceBackgroundVisibleInCampaign(UUID campaignId, Background background) {
        if (background.getHomebrew() == null) {
            return; // core content always visible
        }
        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
        if (!pkgIds.contains(background.getHomebrew().getId())) {
            throw new ResourceNotFoundException("Background not found");
        }
    }

    private void enforceSpellVisibleInCampaign(UUID campaignId, Spell spell) {
        if (spell.getHomebrew() == null) {
            return; // core content always visible
        }
        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
        if (!pkgIds.contains(spell.getHomebrew().getId())) {
            throw new ResourceNotFoundException("Spell not found");
        }
    }

    private void enforceFeatVisibleInCampaign(UUID campaignId, Feat feat) {
        if (feat.getHomebrew() == null) {
            return; // core content always visible
        }
        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
        if (!pkgIds.contains(feat.getHomebrew().getId())) {
            throw new ResourceNotFoundException("Feat not found");
        }
    }

    private void enforceAccess(UUID campaignId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
    }
}
