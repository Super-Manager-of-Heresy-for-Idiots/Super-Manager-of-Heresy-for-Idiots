package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.content.ContentSkill;
import com.dnd.app.dto.response.*;
import com.dnd.app.dto.content.ContentLabelDto;
import com.dnd.app.dto.content.FeatOptionDto;
import com.dnd.app.dto.content.ModifierKeyDto;
import com.dnd.app.config.CacheConfig;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import com.dnd.app.util.Localization;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Класс ReferenceDataService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReferenceDataService {

    private final BackgroundRepository backgroundRepository;
    private final ContentSkillRepository contentSkillRepository;
    private final StatTypeRepository statTypeRepository;
    private final CurrencyTypeRepository currencyTypeRepository;
    private final SpellRepository spellRepository;
    private final FeatRepository featRepository;
    private final RarityRepository rarityRepository;
    private final DamageTypeRepository damageTypeRepository;
    private final BestiaryConditionRepository bestiaryConditionRepository;
    private final SpellSchoolRepository spellSchoolRepository;
    private final CreatureSizeRepository creatureSizeRepository;
    private final TriggerEventTypeRepository triggerEventTypeRepository;
    private final CampaignHomebrewRepository campaignHomebrewRepository;
    private final CampaignService campaignService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // getClasses(...) removed in Phase 12 — class reference now served by ContentReferenceService.

    // getRaces(...) removed in S5 — species reference now served by ContentReferenceService.

    /**
     * Возвращает результат операции "get backgrounds" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<BackgroundResponse> getBackgrounds(UUID campaignId, String username, String lang) {
        enforceAccess(campaignId, username);
        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);

        List<Background> bgs = new ArrayList<>(backgroundRepository.findAllByHomebrewIsNull());
        if (!pkgIds.isEmpty()) {
            bgs.addAll(backgroundRepository.findAllByHomebrewIdIn(pkgIds));
        }

        return bgs.stream().map(bg -> mapBackground(bg, lang)).toList();
    }

    /**
     * Возвращает результат операции "get skills" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<ProficiencySkillResponse> getSkills(UUID campaignId, String username, String lang) {
        enforceAccess(campaignId, username);
        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);

        List<ContentSkill> skills = new ArrayList<>(contentSkillRepository.findAllByHomebrewIsNull());
        if (!pkgIds.isEmpty()) {
            skills.addAll(contentSkillRepository.findAllByHomebrewIdIn(pkgIds));
        }

        return skills.stream().map(s -> mapContentSkill(s, lang)).toList();
    }

    /**
     * Возвращает результат операции "get stat types" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<StatTypeResponse> getStatTypes(UUID campaignId, String username) {
        enforceAccess(campaignId, username);
        return statTypeRepository.findByHomebrewIsNull().stream()
                .map(st -> StatTypeResponse.builder()
                        .id(st.getId())
                        .name(st.getNameRu())
                        .build())
                .toList();
    }

    /**
     * Возвращает результат операции "get currencies" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<CurrencyTypeResponse> getCurrencies(UUID campaignId, String username, String lang) {
        enforceAccess(campaignId, username);
        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);

        List<CurrencyType> currencies = new ArrayList<>(currencyTypeRepository.findByHomebrewIsNull());
        if (!pkgIds.isEmpty()) {
            currencies.addAll(currencyTypeRepository.findByHomebrewIdIn(pkgIds.stream().toList()));
        }

        return currencies.stream()
                .map(ct -> mapCurrency(ct, lang))
                .toList();
    }

    /**
     * Возвращает результат операции "get spells" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param classId идентификатор class, используемый для выбора нужного бизнес-объекта
     * @param level входящее значение level, используемое бизнес-сценарием
     * @param school входящее значение school, используемое бизнес-сценарием
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<SpellResponse> getSpells(UUID campaignId, String username,
                                          UUID classId, Integer level, String school, String lang) {
        enforceAccess(campaignId, username);
        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);

        List<Spell> spells;
        if (pkgIds.isEmpty()) {
            spells = spellRepository.findFilteredSystemOnly(level, school);
        } else {
            spells = spellRepository.findFiltered(pkgIds, level, school);
        }

        return spells.stream().map(s -> mapSpell(s, lang)).toList();
    }

    // --- Vanilla (no-campaign) variants for character templates ---

    // getVanillaClasses(...) removed in Phase 12 — class reference now served by ContentReferenceService.

    // getVanillaRaces(...) removed in S5 — species reference now served by ContentReferenceService.

    /**
     * Возвращает результат операции "get vanilla backgrounds" в рамках бизнес-логики домена.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Cacheable(value = CacheConfig.VANILLA_BACKGROUNDS, key = "#lang")
    @Transactional(readOnly = true)
    public List<BackgroundResponse> getVanillaBackgrounds(String lang) {
        return backgroundRepository.findAllByHomebrewIsNull().stream()
                .map(bg -> mapBackground(bg, lang)).toList();
    }

    /**
     * Возвращает результат операции "get vanilla skills" в рамках бизнес-логики домена.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Cacheable(value = CacheConfig.VANILLA_SKILLS, key = "#lang")
    @Transactional(readOnly = true)
    public List<ProficiencySkillResponse> getVanillaSkills(String lang) {
        return contentSkillRepository.findAllByHomebrewIsNull().stream()
                .map(s -> mapContentSkill(s, lang))
                .toList();
    }

    /**
     * Возвращает результат операции "get vanilla stat types" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    @Cacheable(CacheConfig.VANILLA_STAT_TYPES)
    @Transactional(readOnly = true)
    public List<StatTypeResponse> getVanillaStatTypes() {
        return statTypeRepository.findByHomebrewIsNull().stream()
                .map(st -> StatTypeResponse.builder()
                        .id(st.getId())
                        .name(st.getNameRu())
                        .build())
                .toList();
    }

    /**
     * Возвращает результат операции "get vanilla currencies" в рамках бизнес-логики домена.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Cacheable(value = CacheConfig.VANILLA_CURRENCIES, key = "#lang")
    @Transactional(readOnly = true)
    public List<CurrencyTypeResponse> getVanillaCurrencies(String lang) {
        return currencyTypeRepository.findByHomebrewIsNull().stream()
                .map(ct -> mapCurrency(ct, lang))
                .toList();
    }

    /**
     * Возвращает результат операции "get vanilla spells" в рамках бизнес-логики домена.
     * @param classId идентификатор class, используемый для выбора нужного бизнес-объекта
     * @param level входящее значение level, используемое бизнес-сценарием
     * @param school входящее значение school, используемое бизнес-сценарием
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Cacheable(value = CacheConfig.VANILLA_SPELLS,
            key = "T(java.util.Objects).hash(#classId, #level, #school, #lang)")
    @Transactional(readOnly = true)
    public List<SpellResponse> getVanillaSpells(UUID classId, Integer level, String school, String lang) {
        List<Spell> spells = spellRepository.findFilteredSystemOnly(level, school);
        return spells.stream().map(s -> mapSpell(s, lang)).toList();
    }

    // --- Authoring reference lookups (class builder dropdowns) ---

    /**
     * Возвращает результат операции "get vanilla abilities" в рамках бизнес-логики домена.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<ContentLabelDto> getVanillaAbilities(String lang) {
        return statTypeRepository.findByHomebrewIsNull().stream()
                .map(st -> ContentLabelDto.builder()
                        .id(st.getId())
                        .slug(st.getSlug())
                        .name(Localization.pick(lang, st.getNameRu(), st.getNameEn(), st.getNameRu()))
                        .nameRu(st.getNameRu())
                        .nameEn(st.getNameEn())
                        .build())
                .toList();
    }

    /**
     * Возвращает результат операции "get vanilla feats" в рамках бизнес-логики домена.
     * @param query входящее значение query, используемое бизнес-сценарием
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<FeatOptionDto> getVanillaFeats(String query, String lang) {
        String needle = query == null ? null : query.trim().toLowerCase();
        return featRepository.findAllByHomebrewIsNull().stream()
                .filter(f -> needle == null || needle.isEmpty()
                        || (f.getNameRu() != null && f.getNameRu().toLowerCase().contains(needle))
                        || (f.getNameEn() != null && f.getNameEn().toLowerCase().contains(needle)))
                .map(f -> FeatOptionDto.builder()
                        .id(f.getId())
                        .slug(f.getSlug())
                        .name(Localization.pick(lang, f.getNameRu(), f.getNameEn(), f.getNameRu()))
                        .prerequisiteText(null)
                        .build())
                .toList();
    }

    // --- System dictionary lookups (item/spell/size authoring dropdowns) ---

    /**
     * Возвращает результат операции "get rarities" в рамках бизнес-логики домена.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<ContentLabelDto> getRarities(String lang) {
        return rarityRepository.findByHomebrewIsNullOrderBySortOrderAscNameRuAsc().stream()
                .map(r -> label(lang, r.getId(), r.getSlug(), r.getNameRu(), r.getNameEn()))
                .toList();
    }

    /**
     * Возвращает результат операции "get damage types" в рамках бизнес-логики домена.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<ContentLabelDto> getDamageTypes(String lang) {
        return damageTypeRepository.findByHomebrewIsNullOrderByNameRuAsc().stream()
                .map(d -> label(lang, d.getId(), d.getSlug(), d.getNameRu(), d.getNameEn()))
                .toList();
    }

    /**
     * Возвращает результат операции "get conditions" в рамках бизнес-логики домена.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<ContentLabelDto> getConditions(String lang) {
        return bestiaryConditionRepository.findAll().stream()
                .filter(c -> c.getHomebrew() == null)
                .sorted(Comparator.comparing(BestiaryCondition::getNameRusloc, Comparator.nullsLast(String::compareTo)))
                .map(c -> label(lang, c.getId(), c.getCode(), c.getNameRusloc(), c.getNameEngloc()))
                .toList();
    }

    /**
     * Возвращает результат операции "get spell schools" в рамках бизнес-логики домена.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<ContentLabelDto> getSpellSchools(String lang) {
        return spellSchoolRepository.findAllByOrderByNameRuAsc().stream()
                .map(s -> label(lang, s.getId(), s.getSlug(), s.getNameRu(), s.getNameEn()))
                .toList();
    }

    /**
     * Возвращает результат операции "get sizes" в рамках бизнес-логики домена.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<ContentLabelDto> getSizes(String lang) {
        return creatureSizeRepository.findByHomebrewIsNullOrderByNameRuAsc().stream()
                .map(s -> label(lang, s.getId(), s.getSlug(), s.getNameRu(), s.getNameEn()))
                .toList();
    }

    /**
     * Возвращает словарь событий-триггеров движка для пикера триггера реакции homebrew-заклинания (HB_UX Фаза 1).
     * slug = trigger_event_type.code (стабильный код события); name = отображаемое имя. Русские подписи FE берёт
     * из i18n по коду (fallback — displayName). rest_completed исключён: как триггер реакции он не имеет смысла.
     * @param lang язык (для будущей локализации; сейчас имя = displayName)
     * @return список меток событий по порядку сортировки
     */
    @Transactional(readOnly = true)
    public List<ContentLabelDto> getReactionTriggers(String lang) {
        return triggerEventTypeRepository.findAll().stream()
                .filter(t -> !"rest_completed".equals(t.getCode()))
                .sorted(Comparator.comparing(com.dnd.app.domain.featurerule.TriggerEventType::getSortOrder,
                        Comparator.nullsLast(Integer::compareTo)))
                .map(t -> ContentLabelDto.builder()
                        .slug(t.getCode())
                        .name(t.getDisplayName())
                        .build())
                .toList();
    }

    private ContentLabelDto label(String lang, UUID id, String slug, String nameRu, String nameEn) {
        return ContentLabelDto.builder()
                .id(id)
                .slug(slug)
                .name(Localization.pick(lang, nameRu, nameEn, nameRu))
                .nameRu(nameRu)
                .nameEn(nameEn)
                .build();
    }

    /**
     * Возвращает результат операции "get modifier keys" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    public List<ModifierKeyDto> getModifierKeys() {
        return List.of(
                ModifierKeyDto.builder().key("speed").label("Скорость").defaultUnit("ft").build(),
                ModifierKeyDto.builder().key("ac").label("Класс брони").build(),
                ModifierKeyDto.builder().key("hp_max").label("Максимум хитов").build(),
                ModifierKeyDto.builder().key("initiative").label("Инициатива").build(),
                ModifierKeyDto.builder().key("spell_save_dc").label("Сложность спасброска заклинаний").build(),
                ModifierKeyDto.builder().key("attack").label("Бонус атаки").build());
    }

    // --- Mapping helpers ---

    // mapClassDetail(...) removed in Phase 12 along with the legacy class reference endpoints.

    // mapRaceDetail(...) removed in S5 along with the legacy race reference endpoints.

    /**
     * Преобразует данные операции "map background" в рамках бизнес-логики домена.
     * @param bg входящее значение bg, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public BackgroundResponse mapBackground(Background bg) {
        return mapBackground(bg, Localization.DEFAULT_LANG);
    }

    /**
     * Преобразует данные операции "map background" в рамках бизнес-логики домена.
     * @param bg входящее значение bg, используемое бизнес-сценарием
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public BackgroundResponse mapBackground(Background bg, String lang) {
        return BackgroundResponse.builder()
                .id(bg.getId())
                .name(Localization.pick(lang, bg.getNameRu(), bg.getNameEn(), bg.getNameRu()))
                .description(bg.getDescription())
                .skillProficiencyNames(List.of())
                .build();
    }

    private ProficiencySkillResponse mapContentSkill(ContentSkill s, String lang) {
        StatType ability = s.getAbilityScore();
        return ProficiencySkillResponse.builder()
                .id(s.getId())
                .name(Localization.pick(lang, s.getNameRu(), s.getNameEn(), s.getNameRu()))
                .governingStatId(ability == null ? null : ability.getId())
                .governingStatName(ability == null ? null : ability.getNameRu())
                .build();
    }

    private CurrencyTypeResponse mapCurrency(CurrencyType ct, String lang) {
        BigDecimal exchangeRateToGold = ct.getCopperValue() != null
                ? ct.getCopperValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                : null;
        return CurrencyTypeResponse.builder()
                .id(ct.getId())
                .name(Localization.pick(lang, ct.getNameRu(), ct.getNameEn(), ct.getNameRu()))
                .exchangeRateToGold(exchangeRateToGold)
                .isDefault("gp".equalsIgnoreCase(ct.getSlug()))
                .build();
    }

    private SpellResponse mapSpell(Spell s, String lang) {
        SpellSchool school = s.getSchool();
        return SpellResponse.builder()
                .id(s.getId())
                .name(Localization.pick(lang, s.getNameRu(), s.getNameEn(), s.getNameRu()))
                .nameEn(s.getNameEn())
                .level(s.getLevel())
                .school(school == null ? null : Localization.pick(lang, school.getNameRu(), school.getNameEn(), school.getNameRu()))
                .description(s.getDescription())
                .availableToClassIds(List.of())
                .build();
    }

    /**
     * Выполняет операции "parse json string list" в рамках бизнес-логики домена.
     * @param json входящее значение json, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public List<String> parseJsonStringList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn(
                    "ReferenceDataService#parseJsonStringList failed: operation=reference-json-deserialize, payloadLength={}",
                    json.length(),
                    e);
            return List.of();
        }
    }

    private void enforceAccess(UUID campaignId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
    }
}
