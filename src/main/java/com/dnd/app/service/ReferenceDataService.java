package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.dto.response.*;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public List<CharacterClassDetailResponse> getClasses(UUID campaignId, String username) {
        enforceAccess(campaignId, username);
        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);

        List<CharacterClass> classes = new ArrayList<>(classRepository.findAllByHomebrewIsNull());
        if (!pkgIds.isEmpty()) {
            classes.addAll(classRepository.findAllByHomebrewIdIn(pkgIds));
        }

        List<ProficiencySkill> allSkills = proficiencySkillRepository.findAll();
        Map<String, ProficiencySkill> skillByName = allSkills.stream()
                .collect(Collectors.toMap(ProficiencySkill::getName, s -> s));

        return classes.stream().map(c -> mapClassDetail(c, skillByName)).toList();
    }

    @Transactional(readOnly = true)
    public List<CharacterRaceDetailResponse> getRaces(UUID campaignId, String username) {
        enforceAccess(campaignId, username);
        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);

        List<CharacterRace> races;
        if (pkgIds.isEmpty()) {
            races = raceRepository.findAvailableActiveSystemOnly();
        } else {
            races = raceRepository.findAvailableActive(pkgIds);
        }

        return races.stream().map(this::mapRaceDetail).toList();
    }

    @Transactional(readOnly = true)
    public List<BackgroundResponse> getBackgrounds(UUID campaignId, String username) {
        enforceAccess(campaignId, username);
        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);

        List<Background> bgs = new ArrayList<>(backgroundRepository.findAllByHomebrewIsNull());
        if (!pkgIds.isEmpty()) {
            bgs.addAll(backgroundRepository.findAllByHomebrewIdIn(pkgIds));
        }

        return bgs.stream().map(this::mapBackground).toList();
    }

    @Transactional(readOnly = true)
    public List<ProficiencySkillResponse> getSkills(UUID campaignId, String username) {
        enforceAccess(campaignId, username);
        return proficiencySkillRepository.findAll().stream()
                .map(s -> ProficiencySkillResponse.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .governingStatId(s.getGoverningStat().getId())
                        .governingStatName(s.getGoverningStat().getName())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StatTypeResponse> getStatTypes(UUID campaignId, String username) {
        enforceAccess(campaignId, username);
        return statTypeRepository.findAll().stream()
                .map(st -> StatTypeResponse.builder()
                        .id(st.getId())
                        .name(st.getName())
                        .description(st.getDescription())
                        .isDefault(st.getIsDefault())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CurrencyTypeResponse> getCurrencies(UUID campaignId, String username) {
        enforceAccess(campaignId, username);
        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);

        List<CurrencyType> currencies = new ArrayList<>(currencyTypeRepository.findByHomebrewIsNull());
        if (!pkgIds.isEmpty()) {
            currencies.addAll(currencyTypeRepository.findByHomebrewIdIn(pkgIds.stream().toList()));
        }

        return currencies.stream()
                .map(ct -> CurrencyTypeResponse.builder()
                        .id(ct.getId())
                        .name(ct.getName())
                        .exchangeRateToGold(ct.getExchangeRateToGold())
                        .isDefault(ct.getIsDefault())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SpellResponse> getSpells(UUID campaignId, String username,
                                          UUID classId, Integer level, String school) {
        enforceAccess(campaignId, username);
        Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);

        List<Spell> spells;
        if (pkgIds.isEmpty()) {
            spells = spellRepository.findFilteredSystemOnly(level, school);
        } else {
            spells = spellRepository.findFiltered(pkgIds, level, school);
        }

        if (classId != null) {
            String classIdStr = classId.toString();
            spells = spells.stream()
                    .filter(s -> s.getAvailableToClassIdsJson() != null
                            && s.getAvailableToClassIdsJson().contains(classIdStr))
                    .toList();
        }

        return spells.stream().map(this::mapSpell).toList();
    }

    // --- Mapping helpers ---

    private CharacterClassDetailResponse mapClassDetail(CharacterClass c, Map<String, ProficiencySkill> skillByName) {
        List<String> savingThrowNames = parseJsonStringList(c.getSavingThrowStatIdsJson());
        List<String> skillOptionNames = parseJsonStringList(c.getSkillChoiceOptionIdsJson());

        List<ProficiencySkillResponse> skillOptions = skillOptionNames.stream()
                .map(name -> {
                    ProficiencySkill ps = skillByName.get(name);
                    if (ps == null) return null;
                    return ProficiencySkillResponse.builder()
                            .id(ps.getId())
                            .name(ps.getName())
                            .governingStatId(ps.getGoverningStat().getId())
                            .governingStatName(ps.getGoverningStat().getName())
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();

        CharacterClassDetailResponse.SpellcastingInfo spellcasting = null;
        if (Boolean.TRUE.equals(c.getIsSpellcaster())) {
            spellcasting = CharacterClassDetailResponse.SpellcastingInfo.builder()
                    .isSpellcaster(true)
                    .spellcastingStatId(c.getSpellcastingStat() != null ? c.getSpellcastingStat().getId() : null)
                    .spellcastingStatName(c.getSpellcastingStat() != null ? c.getSpellcastingStat().getName() : null)
                    .hasCantrips(c.getHasCantrips())
                    .isHalfCaster(c.getIsHalfCaster())
                    .build();
        }

        return CharacterClassDetailResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .hitDie(c.getHitDie())
                .primaryAbilityStatId(c.getPrimaryAbilityStat() != null ? c.getPrimaryAbilityStat().getId() : null)
                .savingThrowStatNames(savingThrowNames)
                .skillChoiceCount(c.getSkillChoiceCount())
                .skillChoiceOptions(skillOptions)
                .armorWeaponProficiencies(c.getArmorWeaponProficiencies())
                .spellcasting(spellcasting)
                .build();
    }

    private CharacterRaceDetailResponse mapRaceDetail(CharacterRace r) {
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
                        traits.add((String) t.get("name"));
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
                    String linName = (String) lin.get("name");
                    String linDesc = (String) lin.get("description");

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
                .name(r.getName())
                .description(r.getDescription())
                .speed(walkSpeed)
                .abilityScoreIncreases(asis)
                .traits(traits)
                .subraces(subraces)
                .build();
    }

    public BackgroundResponse mapBackground(Background bg) {
        List<String> skillNames = parseJsonStringList(bg.getSkillProficiencyIdsJson());
        return BackgroundResponse.builder()
                .id(bg.getId())
                .name(bg.getName())
                .description(bg.getDescription())
                .skillProficiencyNames(skillNames)
                .grantedExtras(bg.getGrantedExtras())
                .build();
    }

    private SpellResponse mapSpell(Spell s) {
        List<UUID> classIds = new ArrayList<>();
        if (s.getAvailableToClassIdsJson() != null) {
            try {
                var ids = objectMapper.readValue(s.getAvailableToClassIdsJson(),
                        new TypeReference<List<String>>() {});
                classIds = ids.stream().map(UUID::fromString).toList();
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
