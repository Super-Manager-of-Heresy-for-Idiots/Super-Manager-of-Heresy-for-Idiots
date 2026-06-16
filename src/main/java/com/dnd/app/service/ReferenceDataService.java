package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.dto.response.*;
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

    private final CharacterClassRepository classRepository;
    private final CharacterRaceRepository raceRepository;
    private final BackgroundRepository backgroundRepository;
    private final ProficiencySkillRepository proficiencySkillRepository;
    private final StatTypeRepository statTypeRepository;
    private final CurrencyTypeRepository currencyTypeRepository;
    private final SpellRepository spellRepository;
    private final CampaignHomebrewRepository campaignHomebrewRepository;
    private final CampaignService campaignService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<CharacterClassDetailResponse> getClasses(UUID campaignId, String username, String lang) {
        enforceAccess(campaignId, username);
        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);

        List<CharacterClass> classes = new ArrayList<>(classRepository.findAllByHomebrewIsNull());
        if (!pkgIds.isEmpty()) {
            classes.addAll(classRepository.findAllByHomebrewIdIn(pkgIds));
        }

        List<ProficiencySkill> allSkills = proficiencySkillRepository.findAll();
        Map<String, ProficiencySkill> skillByName = allSkills.stream()
                .collect(Collectors.toMap(ProficiencySkill::getName, s -> s));

        return classes.stream().map(c -> mapClassDetail(c, skillByName, lang)).toList();
    }

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

    @Cacheable(value = CacheConfig.VANILLA_CLASSES, key = "#lang")
    @Transactional(readOnly = true)
    public List<CharacterClassDetailResponse> getVanillaClasses(String lang) {
        List<CharacterClass> classes = classRepository.findAllByHomebrewIsNull();
        Map<String, ProficiencySkill> skillByName = proficiencySkillRepository.findAll().stream()
                .collect(Collectors.toMap(ProficiencySkill::getName, s -> s));
        return classes.stream().map(c -> mapClassDetail(c, skillByName, lang)).toList();
    }

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

    // --- Mapping helpers ---

    private CharacterClassDetailResponse mapClassDetail(CharacterClass c, Map<String, ProficiencySkill> skillByName, String lang) {
        List<String> savingThrowNames = parseJsonStringList(c.getSavingThrowStatIdsJson());
        List<String> skillOptionNames = parseJsonStringList(c.getSkillChoiceOptionIdsJson());

        List<ProficiencySkillResponse> skillOptions = skillOptionNames.stream()
                .map(name -> {
                    ProficiencySkill ps = skillByName.get(name);
                    if (ps == null) return null;
                    return mapProficiencySkill(ps, lang);
                })
                .filter(Objects::nonNull)
                .toList();

        CharacterClassDetailResponse.SpellcastingInfo spellcasting = null;
        if (Boolean.TRUE.equals(c.getIsSpellcaster())) {
            spellcasting = CharacterClassDetailResponse.SpellcastingInfo.builder()
                    .isSpellcaster(true)
                    .spellcastingStatId(c.getSpellcastingStat() != null ? c.getSpellcastingStat().getId() : null)
                    .spellcastingStatName(c.getSpellcastingStat() != null ? c.getSpellcastingStat().getNameRu() : null)
                    .hasCantrips(c.getHasCantrips())
                    .isHalfCaster(c.getIsHalfCaster())
                    .build();
        }

        return CharacterClassDetailResponse.builder()
                .id(c.getId())
                .name(Localization.pick(lang, c.getNameRusloc(), c.getNameEngloc(), c.getName()))
                .description(Localization.pick(lang, c.getDescriptionRusloc(), c.getDescriptionEngloc(), c.getDescription()))
                .hitDie(c.getHitDie())
                .primaryAbilityStatId(c.getPrimaryAbilityStat() != null ? c.getPrimaryAbilityStat().getId() : null)
                .savingThrowStatNames(savingThrowNames)
                .skillChoiceCount(c.getSkillChoiceCount())
                .skillChoiceOptions(skillOptions)
                .armorWeaponProficiencies(Localization.pick(lang,
                        c.getArmorWeaponProficienciesRusloc(),
                        c.getArmorWeaponProficienciesEngloc(),
                        c.getArmorWeaponProficiencies()))
                .spellcasting(spellcasting)
                .build();
    }

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
