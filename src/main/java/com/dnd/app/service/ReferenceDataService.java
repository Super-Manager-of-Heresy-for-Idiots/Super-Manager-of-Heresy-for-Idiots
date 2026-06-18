package com.dnd.app.service;

import com.dnd.app.domain.*;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferenceDataService {

    private final CharacterRaceRepository raceRepository;
    private final BackgroundRepository backgroundRepository;
    private final ProficiencySkillRepository proficiencySkillRepository;
    private final StatTypeRepository statTypeRepository;
    private final CurrencyTypeRepository currencyTypeRepository;
    private final SpellRepository spellRepository;
    private final FeatRepository featRepository;
    private final RarityRepository rarityRepository;
    private final DamageTypeRepository damageTypeRepository;
    private final SpellSchoolRepository spellSchoolRepository;
    private final CreatureSizeRepository creatureSizeRepository;
    private final CampaignHomebrewRepository campaignHomebrewRepository;
    private final CampaignService campaignService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // getClasses(...) removed in Phase 12 — class reference now served by ContentReferenceService.

    @Transactional(readOnly = true)
    public List<CharacterRaceDetailResponse> getRaces(UUID campaignId, String username, String lang) {
        enforceAccess(campaignId, username);
        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);

        List<CharacterRace> races;
        if (pkgIds.isEmpty()) {
            races = raceRepository.findAvailableActiveSystemOnly();
        } else {
            races = raceRepository.findAvailableActive(pkgIds);
        }

        return races.stream().map(r -> mapRaceDetail(r, lang)).toList();
    }

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

    @Transactional(readOnly = true)
    public List<ProficiencySkillResponse> getSkills(UUID campaignId, String username, String lang) {
        enforceAccess(campaignId, username);
        return proficiencySkillRepository.findAll().stream()
                .map(s -> mapProficiencySkill(s, lang))
                .toList();
    }

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

    @Cacheable(value = CacheConfig.VANILLA_RACES, key = "#lang")
    @Transactional(readOnly = true)
    public List<CharacterRaceDetailResponse> getVanillaRaces(String lang) {
        return raceRepository.findAvailableActiveSystemOnly().stream()
                .map(r -> mapRaceDetail(r, lang)).toList();
    }

    @Cacheable(value = CacheConfig.VANILLA_BACKGROUNDS, key = "#lang")
    @Transactional(readOnly = true)
    public List<BackgroundResponse> getVanillaBackgrounds(String lang) {
        return backgroundRepository.findAllByHomebrewIsNull().stream()
                .map(bg -> mapBackground(bg, lang)).toList();
    }

    @Cacheable(value = CacheConfig.VANILLA_SKILLS, key = "#lang")
    @Transactional(readOnly = true)
    public List<ProficiencySkillResponse> getVanillaSkills(String lang) {
        return proficiencySkillRepository.findAll().stream()
                .map(s -> mapProficiencySkill(s, lang))
                .toList();
    }

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

    @Cacheable(value = CacheConfig.VANILLA_CURRENCIES, key = "#lang")
    @Transactional(readOnly = true)
    public List<CurrencyTypeResponse> getVanillaCurrencies(String lang) {
        return currencyTypeRepository.findByHomebrewIsNull().stream()
                .map(ct -> mapCurrency(ct, lang))
                .toList();
    }

    @Cacheable(value = CacheConfig.VANILLA_SPELLS,
            key = "T(java.util.Objects).hash(#classId, #level, #school, #lang)")
    @Transactional(readOnly = true)
    public List<SpellResponse> getVanillaSpells(UUID classId, Integer level, String school, String lang) {
        List<Spell> spells = spellRepository.findFilteredSystemOnly(level, school);
        return spells.stream().map(s -> mapSpell(s, lang)).toList();
    }

    // --- Authoring reference lookups (class builder dropdowns) ---

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

    @Transactional(readOnly = true)
    public List<ContentLabelDto> getRarities(String lang) {
        return rarityRepository.findByHomebrewIsNullOrderBySortOrderAscNameRuAsc().stream()
                .map(r -> label(lang, r.getId(), r.getSlug(), r.getNameRu(), r.getNameEn()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ContentLabelDto> getDamageTypes(String lang) {
        return damageTypeRepository.findByHomebrewIsNullOrderByNameRuAsc().stream()
                .map(d -> label(lang, d.getId(), d.getSlug(), d.getNameRu(), d.getNameEn()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ContentLabelDto> getSpellSchools(String lang) {
        return spellSchoolRepository.findAllByOrderByNameRuAsc().stream()
                .map(s -> label(lang, s.getId(), s.getSlug(), s.getNameRu(), s.getNameEn()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ContentLabelDto> getSizes(String lang) {
        return creatureSizeRepository.findByHomebrewIsNullOrderByNameRuAsc().stream()
                .map(s -> label(lang, s.getId(), s.getSlug(), s.getNameRu(), s.getNameEn()))
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

    private CharacterRaceDetailResponse mapRaceDetail(CharacterRace r, String lang) {
        Integer walkSpeed = null;
        if (r.getSpeedJson() != null) {
            try {
                var speedMap = objectMapper.readValue(r.getSpeedJson(), new TypeReference<Map<String, Integer>>() {});
                walkSpeed = speedMap.getOrDefault("walk", 30);
            } catch (Exception e) {
                walkSpeed = 30;
            }
        }

        List<CharacterRaceDetailResponse.AbilityScoreIncrease> asis = new ArrayList<>();
        if (r.getAbilityScoreBonusesJson() != null) {
            try {
                var bonuses = objectMapper.readValue(r.getAbilityScoreBonusesJson(),
                        new TypeReference<List<Map<String, Object>>>() {});
                for (var b : bonuses) {
                    asis.add(CharacterRaceDetailResponse.AbilityScoreIncrease.builder()
                            .statName((String) b.get("ability"))
                            .bonus(((Number) b.get("value")).intValue())
                            .build());
                }
            } catch (Exception ignored) {}
        }

        List<String> traits = new ArrayList<>();
        if (r.getTraitsJson() != null) {
            try {
                var traitList = objectMapper.readValue(r.getTraitsJson(),
                        new TypeReference<List<Map<String, Object>>>() {});
                for (var t : traitList) {
                    if (t.containsKey("name")) {
                        traits.add(Localization.pick(lang,
                                (String) t.get("nameRusloc"),
                                (String) t.get("nameEngloc"),
                                (String) t.get("name")));
                    }
                }
            } catch (Exception ignored) {}
        }

        List<CharacterRaceDetailResponse.SubraceInfo> subraces = new ArrayList<>();
        if (r.getLineagesJson() != null) {
            try {
                var lineages = objectMapper.readValue(r.getLineagesJson(),
                        new TypeReference<List<Map<String, Object>>>() {});
                for (var lin : lineages) {
                    String id = lin.get("id") != null ? lin.get("id").toString() : null;
                    String linName = Localization.pick(lang,
                            (String) lin.get("nameRusloc"),
                            (String) lin.get("nameEngloc"),
                            (String) lin.get("name"));
                    String linDesc = Localization.pick(lang,
                            (String) lin.get("descriptionRusloc"),
                            (String) lin.get("descriptionEngloc"),
                            (String) lin.get("description"));

                    List<CharacterRaceDetailResponse.AbilityScoreIncrease> subAsis = new ArrayList<>();
                    List<String> subTraits = new ArrayList<>();

                    subraces.add(CharacterRaceDetailResponse.SubraceInfo.builder()
                            .id(id != null ? UUID.fromString(id) : null)
                            .name(linName)
                            .description(linDesc)
                            .abilityScoreIncreases(subAsis)
                            .traits(subTraits)
                            .build());
                }
            } catch (Exception ignored) {}
        }

        return CharacterRaceDetailResponse.builder()
                .id(r.getId())
                .name(Localization.pick(lang, r.getNameRusloc(), r.getNameEngloc(), r.getName()))
                .description(Localization.pick(lang, r.getDescriptionRusloc(), r.getDescriptionEngloc(), r.getDescription()))
                .speed(walkSpeed)
                .abilityScoreIncreases(asis)
                .traits(traits)
                .subraces(subraces)
                .build();
    }

    /** Canonical (English) mapping for callers without a UI-language context. */
    public BackgroundResponse mapBackground(Background bg) {
        return mapBackground(bg, Localization.DEFAULT_LANG);
    }

    public BackgroundResponse mapBackground(Background bg, String lang) {
        return BackgroundResponse.builder()
                .id(bg.getId())
                .name(Localization.pick(lang, bg.getNameRu(), bg.getNameEn(), bg.getNameRu()))
                .description(bg.getDescription())
                .skillProficiencyNames(List.of())
                .build();
    }

    private ProficiencySkillResponse mapProficiencySkill(ProficiencySkill s, String lang) {
        return ProficiencySkillResponse.builder()
                .id(s.getId())
                .name(Localization.pick(lang, s.getNameRusloc(), s.getNameEngloc(), s.getName()))
                .governingStatId(s.getGoverningStat().getId())
                .governingStatName(s.getGoverningStat().getNameRu())
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

    public List<String> parseJsonStringList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
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
