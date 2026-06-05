package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.*;
import com.dnd.app.dto.request.*;
import com.dnd.app.dto.response.CharacterRaceSnapshotResponse;
import com.dnd.app.dto.response.RaceListItemResponse;
import com.dnd.app.dto.response.RaceResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class RaceService {

    private static final String PHB_2024 = "Player's Handbook 2024";

    private final CharacterRaceRepository raceRepository;
    private final PlayerCharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final HomebrewPackageRepository homebrewPackageRepository;
    private final HomebrewContentItemRepository contentItemRepository;
    private final CampaignHomebrewRepository campaignHomebrewRepository;
    private final CampaignService campaignService;
    private final ObjectMapper objectMapper;

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
    private static final TypeReference<List<RaceTraitRequest>> TRAIT_LIST = new TypeReference<>() {};
    private static final TypeReference<List<RaceLineageRequest>> LINEAGE_LIST = new TypeReference<>() {};
    private static final TypeReference<List<RaceAbilityScoreBonusDto>> BONUS_LIST = new TypeReference<>() {};

    @Transactional(readOnly = true)
    public List<RaceListItemResponse> listAdminRaces() {
        return raceRepository.findAll().stream().map(this::toListItem).toList();
    }

    @Transactional(readOnly = true)
    public List<RaceListItemResponse> listAvailableForCampaign(UUID campaignId, String username) {
        Campaign campaign = campaignService.findCampaign(campaignId);
        User user = getUser(username);
        campaignService.enforceMembershipOrAdmin(campaign, user);

        Set<UUID> packageIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
        List<CharacterRace> races = packageIds.isEmpty()
                ? raceRepository.findAvailableActiveSystemOnly()
                : raceRepository.findAvailableActive(packageIds);
        return races.stream().map(this::toListItem).toList();
    }

    @Transactional(readOnly = true)
    public RaceResponse getRace(UUID raceId, String username) {
        getUser(username);
        CharacterRace race = getRaceOrThrow(raceId);
        return toResponse(race);
    }

    @Transactional(readOnly = true)
    public RaceResponse getAvailableRace(UUID campaignId, UUID raceId, String username) {
        Campaign campaign = campaignService.findCampaign(campaignId);
        User user = getUser(username);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        CharacterRace race = getSelectableRace(campaignId, raceId);
        return toResponse(race);
    }

    @Transactional
    public RaceResponse createSystemRace(RaceCreateRequest request, String username) {
        User admin = requireRole(username, Role.ADMIN);
        RaceCreateRequest normalized = normalizeSource(request, RaceSourceType.SYSTEM);
        CharacterRace race = new CharacterRace();
        applyRequest(race, normalized, admin, null);
        CharacterRace saved = raceRepository.save(race);
        log.info("System race created: id={}, name='{}', by={}", saved.getId(), saved.getName(), username);
        return toResponse(saved);
    }

    @Transactional
    public RaceResponse updateSystemRace(UUID raceId, RaceUpdateRequest request, String username) {
        User admin = requireRole(username, Role.ADMIN);
        CharacterRace race = getRaceOrThrow(raceId);
        if (race.getSourceType() != RaceSourceType.SYSTEM || race.getHomebrew() != null) {
            throw new BadRequestException("Only SYSTEM races can be updated through admin system endpoint");
        }
        RaceCreateRequest normalized = normalizeSource(request, RaceSourceType.SYSTEM);
        applyRequest(race, normalized, admin, null);
        return toResponse(raceRepository.save(race));
    }

    @Transactional
    public RaceResponse setSystemRaceActive(UUID raceId, boolean active, String username) {
        User admin = requireRole(username, Role.ADMIN);
        CharacterRace race = getRaceOrThrow(raceId);
        if (race.getSourceType() != RaceSourceType.SYSTEM || race.getHomebrew() != null) {
            throw new BadRequestException("Only SYSTEM races can be managed through admin system endpoint");
        }
        race.setActive(active);
        race.setUpdatedBy(admin);
        return toResponse(raceRepository.save(race));
    }

    @Transactional
    public RaceResponse softDeleteSystemRace(UUID raceId, String username) {
        return setSystemRaceActive(raceId, false, username);
    }

    @Transactional
    public RaceResponse createHomebrewRace(UUID packageId, RaceCreateRequest request, String username) {
        User gm = requireGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);
        RaceCreateRequest normalized = normalizeSource(request, RaceSourceType.HOMEBREW);
        CharacterRace race = new CharacterRace();
        applyRequest(race, normalized, gm, pkg);
        CharacterRace saved = raceRepository.save(race);
        attachToPackage(pkg, saved.getId());
        log.info("Homebrew race created: packageId={}, raceId={}, by={}", packageId, saved.getId(), username);
        return toResponse(saved);
    }

    @Transactional
    public RaceResponse updateHomebrewRace(UUID packageId, UUID raceId, RaceUpdateRequest request, String username) {
        User gm = requireGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);
        CharacterRace race = getRaceOrThrow(raceId);
        requireSamePackage(pkg, race);
        RaceCreateRequest normalized = normalizeSource(request, RaceSourceType.HOMEBREW);
        applyRequest(race, normalized, gm, pkg);
        return toResponse(raceRepository.save(race));
    }

    @Transactional
    public RaceResponse setHomebrewRaceActive(UUID packageId, UUID raceId, boolean active, String username) {
        User gm = requireGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);
        CharacterRace race = getRaceOrThrow(raceId);
        requireSamePackage(pkg, race);
        race.setActive(active);
        race.setUpdatedBy(gm);
        return toResponse(raceRepository.save(race));
    }

    @Transactional
    public RaceResponse softDeleteHomebrewRace(UUID packageId, UUID raceId, String username) {
        return setHomebrewRaceActive(packageId, raceId, false, username);
    }

    @Transactional
    public RaceResponse duplicateSystemRaceIntoHomebrew(UUID packageId, UUID raceId, String username) {
        User gm = requireGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);
        CharacterRace source = getRaceOrThrow(raceId);
        if (source.getSourceType() != RaceSourceType.SYSTEM || source.getHomebrew() != null) {
            throw new BadRequestException("Only SYSTEM race can be duplicated into homebrew");
        }

        CharacterRace copy = new CharacterRace();
        copy.setName(nextCopyName(source.getName(), pkg.getTitle()));
        copy.setSlug(null);
        copy.setDescription(source.getDescription());
        copy.setLoreDescription(source.getLoreDescription());
        copy.setSourceType(RaceSourceType.HOMEBREW);
        copy.setSourceName("Homebrew copy of " + nullToEmpty(source.getSourceName()));
        copy.setActive(true);
        copy.setCreatedBy(gm);
        copy.setUpdatedBy(gm);
        copy.setHomebrew(pkg);
        copy.setCreatureType(source.getCreatureType());
        copy.setSizeOptionsJson(source.getSizeOptionsJson());
        copy.setDefaultSize(source.getDefaultSize());
        copy.setSpeedJson(source.getSpeedJson());
        copy.setDarkvisionRange(source.getDarkvisionRange());
        copy.setTraitsJson(source.getTraitsJson());
        copy.setLineagesJson(source.getLineagesJson());
        copy.setLineageRequired(source.getLineageRequired());
        copy.setLanguagesJson(source.getLanguagesJson());
        copy.setLanguageOptionsJson(source.getLanguageOptionsJson());
        copy.setProficienciesJson(source.getProficienciesJson());
        copy.setResistancesJson(source.getResistancesJson());
        copy.setVulnerabilitiesJson(source.getVulnerabilitiesJson());
        copy.setImmunitiesJson(source.getImmunitiesJson());
        copy.setConditionResistancesJson(source.getConditionResistancesJson());
        copy.setConditionAdvantagesJson(source.getConditionAdvantagesJson());
        copy.setInnateSpellsJson(source.getInnateSpellsJson());
        copy.setAllowAbilityScoreBonuses(source.getAllowAbilityScoreBonuses());
        copy.setAbilityScoreBonusesJson(source.getAbilityScoreBonusesJson());
        copy.setMetadataJson(source.getMetadataJson());

        CharacterRace saved = raceRepository.save(copy);
        attachToPackage(pkg, saved.getId());
        log.info("System race duplicated into homebrew: sourceRaceId={}, newRaceId={}, packageId={}, by={}",
                raceId, saved.getId(), packageId, username);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CharacterRace getSelectableRace(UUID campaignId, UUID raceId) {
        CharacterRace race = getRaceOrThrow(raceId);
        if (!Boolean.TRUE.equals(race.getActive())) {
            throw new BadRequestException("Selected race is disabled");
        }
        if (race.getHomebrew() == null) {
            return race;
        }
        Set<UUID> packageIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
        if (!packageIds.contains(race.getHomebrew().getId())) {
            throw new BadRequestException("Selected race is not available in this campaign");
        }
        return race;
    }

    public void validateLineageSelection(CharacterRace race, UUID selectedLineageId) {
        List<RaceLineageRequest> lineages = read(race.getLineagesJson(), LINEAGE_LIST, List.of());
        if (Boolean.TRUE.equals(race.getLineageRequired()) && !lineages.isEmpty() && selectedLineageId == null) {
            throw new BadRequestException("Selected race requires lineage/subrace selection");
        }
        if (selectedLineageId != null && lineages.stream().noneMatch(l -> selectedLineageId.equals(l.getId()))) {
            throw new BadRequestException("Selected lineage does not belong to selected race");
        }
    }

    public String buildRaceSnapshotJson(CharacterRace race, UUID selectedLineageId) {
        CharacterRaceSnapshotResponse snapshot = buildRaceSnapshot(race, selectedLineageId);
        return write(snapshot);
    }

    public CharacterRaceSnapshotResponse parseSnapshot(String snapshotJson) {
        return read(snapshotJson, new TypeReference<CharacterRaceSnapshotResponse>() {}, null);
    }

    private CharacterRaceSnapshotResponse buildRaceSnapshot(CharacterRace race, UUID selectedLineageId) {
        List<RaceTraitRequest> traits = read(race.getTraitsJson(), TRAIT_LIST, List.of());
        List<RaceLineageRequest> lineages = read(race.getLineagesJson(), LINEAGE_LIST, List.of());
        RaceLineageRequest selectedLineage = selectedLineageId == null ? null : lineages.stream()
                .filter(l -> selectedLineageId.equals(l.getId()))
                .findFirst()
                .orElse(null);
        List<String> traitNames = Stream.concat(
                        traits.stream().map(RaceTraitRequest::getName),
                        selectedLineage != null && selectedLineage.getTraits() != null
                                ? selectedLineage.getTraits().stream().map(RaceTraitRequest::getName)
                                : Stream.empty())
                .filter(Objects::nonNull)
                .toList();
        return CharacterRaceSnapshotResponse.builder()
                .raceId(race.getId())
                .raceName(race.getName())
                .lineageId(selectedLineageId)
                .lineageName(selectedLineage != null ? selectedLineage.getName() : null)
                .size(race.getDefaultSize())
                .speed(read(race.getSpeedJson(), new TypeReference<RaceSpeedDto>() {}, null))
                .darkvisionRange(race.getDarkvisionRange())
                .traitNames(traitNames)
                .allowAbilityScoreBonuses(race.getAllowAbilityScoreBonuses())
                .build();
    }

    private void applyRequest(CharacterRace race, RaceCreateRequest request, User actor, HomebrewPackage pkg) {
        validateRequest(request);
        validateUniqueness(race, request);

        race.setName(request.getName());
        race.setSlug(request.getSlug());
        race.setDescription(request.getDescription());
        race.setLoreDescription(request.getLoreDescription());
        race.setSourceType(RaceSourceType.valueOf(request.getSourceType()));
        race.setSourceName(request.getSourceName());
        race.setActive(request.getActive() != null ? request.getActive() : true);
        race.setUpdatedBy(actor);
        if (race.getCreatedBy() == null) {
            race.setCreatedBy(actor);
        }
        race.setHomebrew(pkg);
        race.setCreatureType(request.getCreatureType());
        race.setSizeOptionsJson(write(withDefaultList(request.getSizeOptions())));
        race.setDefaultSize(request.getDefaultSize() != null ? request.getDefaultSize() : request.getSizeOptions().getFirst());
        race.setSpeedJson(write(request.getSpeed()));
        race.setDarkvisionRange(request.getDarkvisionRange());
        race.setTraitsJson(write(withGeneratedTraitIds(request.getTraits())));
        race.setLineagesJson(write(withGeneratedLineageIds(request.getLineageOptions())));
        race.setLineageRequired(request.getLineageRequired() != null ? request.getLineageRequired() : false);
        race.setLanguagesJson(write(withDefaultList(request.getLanguages())));
        race.setLanguageOptionsJson(write(request.getLanguageOptions()));
        race.setProficienciesJson(write(withDefaultList(request.getProficiencies())));
        race.setResistancesJson(write(withDefaultList(request.getResistances())));
        race.setVulnerabilitiesJson(write(withDefaultList(request.getVulnerabilities())));
        race.setImmunitiesJson(write(withDefaultList(request.getImmunities())));
        race.setConditionResistancesJson(write(withDefaultList(request.getConditionResistances())));
        race.setConditionAdvantagesJson(write(withDefaultList(request.getConditionAdvantages())));
        race.setInnateSpellsJson(write(request.getInnateSpells()));
        race.setAllowAbilityScoreBonuses(request.getAllowAbilityScoreBonuses() != null
                ? request.getAllowAbilityScoreBonuses()
                : false);
        race.setAbilityScoreBonusesJson(write(withDefaultList(request.getAbilityScoreBonuses())));
        race.setMetadataJson(write(request.getMetadata()));
    }

    private void validateRequest(RaceCreateRequest request) {
        RaceSourceType sourceType = parseEnum(RaceSourceType.class, request.getSourceType(), "sourceType");
        request.getSizeOptions().forEach(size -> parseEnum(CreatureSize.class, size, "sizeOptions"));
        if (request.getDefaultSize() != null && !request.getSizeOptions().contains(request.getDefaultSize())) {
            throw new BadRequestException("defaultSize must be one of sizeOptions");
        }
        boolean allowBonuses = Boolean.TRUE.equals(request.getAllowAbilityScoreBonuses());
        if (!allowBonuses && request.getAbilityScoreBonuses() != null && !request.getAbilityScoreBonuses().isEmpty()) {
            throw new BadRequestException("abilityScoreBonuses are allowed only when allowAbilityScoreBonuses=true");
        }
        if (sourceType == RaceSourceType.SYSTEM && PHB_2024.equals(request.getSourceName()) && allowBonuses) {
            throw new BadRequestException("PHB 2024 SYSTEM races must not define ability score bonuses");
        }
        if (request.getAbilityScoreBonuses() != null) {
            for (RaceAbilityScoreBonusDto bonus : request.getAbilityScoreBonuses()) {
                parseEnum(Ability.class, bonus.getAbility(), "abilityScoreBonuses.ability");
                parseEnum(RaceAbilityBonusMode.class, bonus.getMode(), "abilityScoreBonuses.mode");
            }
        }
        validateTraits(request.getTraits());
        if (request.getLineageOptions() != null) {
            for (RaceLineageRequest lineage : request.getLineageOptions()) {
                validateTraits(lineage.getTraits());
            }
        }
    }

    private void validateTraits(List<RaceTraitRequest> traits) {
        if (traits == null) return;
        for (RaceTraitRequest trait : traits) {
            if (trait.getUses() != null) {
                if (trait.getUses().getType() != null) {
                    parseEnum(RaceTraitUseType.class, trait.getUses().getType(), "trait.uses.type");
                }
                if (trait.getUses().getRecharge() != null) {
                    parseEnum(RaceTraitRecharge.class, trait.getUses().getRecharge(), "trait.uses.recharge");
                }
            }
            if (trait.getActionType() != null) {
                parseEnum(RaceTraitActionType.class, trait.getActionType(), "trait.actionType");
            }
            if (trait.getDamage() != null && trait.getDamage().getDamageType() != null) {
                parseEnum(DamageType.class, trait.getDamage().getDamageType(), "trait.damage.damageType");
            }
            if (trait.getSavingThrow() != null && trait.getSavingThrow().getAbility() != null) {
                parseEnum(Ability.class, trait.getSavingThrow().getAbility(), "trait.savingThrow.ability");
            }
        }
    }

    private void validateUniqueness(CharacterRace race, RaceCreateRequest request) {
        boolean nameChanged = race.getId() == null || !race.getName().equals(request.getName());
        if (nameChanged && raceRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Race with this name already exists");
        }
        if (request.getSlug() != null) {
            boolean slugChanged = race.getId() == null || race.getSlug() == null || !race.getSlug().equals(request.getSlug());
            if (slugChanged && raceRepository.existsBySlug(request.getSlug())) {
                throw new DuplicateResourceException("Race with this slug already exists");
            }
        }
    }

    private RaceCreateRequest normalizeSource(RaceCreateRequest request, RaceSourceType sourceType) {
        request.setSourceType(sourceType.name());
        if (request.getCreatureType() == null) {
            request.setCreatureType("HUMANOID");
        }
        return request;
    }

    private RaceListItemResponse toListItem(CharacterRace race) {
        return RaceListItemResponse.builder()
                .id(race.getId())
                .name(race.getName())
                .slug(race.getSlug())
                .description(race.getDescription())
                .sourceType(race.getSourceType().name())
                .sourceName(race.getSourceName())
                .active(race.getActive())
                .homebrewId(race.getHomebrew() != null ? race.getHomebrew().getId() : null)
                .homebrewTitle(race.getHomebrew() != null ? race.getHomebrew().getTitle() : null)
                .lineageRequired(race.getLineageRequired())
                .allowAbilityScoreBonuses(race.getAllowAbilityScoreBonuses())
                .build();
    }

    public RaceResponse toResponse(CharacterRace race) {
        return RaceResponse.builder()
                .id(race.getId())
                .name(race.getName())
                .slug(race.getSlug())
                .description(race.getDescription())
                .loreDescription(race.getLoreDescription())
                .sourceType(race.getSourceType().name())
                .sourceName(race.getSourceName())
                .active(race.getActive())
                .createdBy(race.getCreatedBy() != null ? race.getCreatedBy().getId() : null)
                .createdByUsername(race.getCreatedBy() != null ? race.getCreatedBy().getUsername() : null)
                .updatedBy(race.getUpdatedBy() != null ? race.getUpdatedBy().getId() : null)
                .updatedByUsername(race.getUpdatedBy() != null ? race.getUpdatedBy().getUsername() : null)
                .homebrewId(race.getHomebrew() != null ? race.getHomebrew().getId() : null)
                .homebrewTitle(race.getHomebrew() != null ? race.getHomebrew().getTitle() : null)
                .creatureType(race.getCreatureType())
                .sizeOptions(read(race.getSizeOptionsJson(), STRING_LIST, List.of()))
                .defaultSize(race.getDefaultSize())
                .speed(read(race.getSpeedJson(), new TypeReference<RaceSpeedDto>() {}, null))
                .darkvisionRange(race.getDarkvisionRange())
                .traits(read(race.getTraitsJson(), TRAIT_LIST, List.of()))
                .lineageOptions(read(race.getLineagesJson(), LINEAGE_LIST, List.of()))
                .lineageRequired(race.getLineageRequired())
                .languages(read(race.getLanguagesJson(), STRING_LIST, List.of()))
                .languageOptions(readJsonNode(race.getLanguageOptionsJson()))
                .proficiencies(read(race.getProficienciesJson(), STRING_LIST, List.of()))
                .resistances(read(race.getResistancesJson(), STRING_LIST, List.of()))
                .vulnerabilities(read(race.getVulnerabilitiesJson(), STRING_LIST, List.of()))
                .immunities(read(race.getImmunitiesJson(), STRING_LIST, List.of()))
                .conditionResistances(read(race.getConditionResistancesJson(), STRING_LIST, List.of()))
                .conditionAdvantages(read(race.getConditionAdvantagesJson(), STRING_LIST, List.of()))
                .innateSpells(readJsonNode(race.getInnateSpellsJson()))
                .allowAbilityScoreBonuses(race.getAllowAbilityScoreBonuses())
                .abilityScoreBonuses(read(race.getAbilityScoreBonusesJson(), BONUS_LIST, List.of()))
                .metadata(readJsonNode(race.getMetadataJson()))
                .createdAt(race.getCreatedAt())
                .updatedAt(race.getUpdatedAt())
                .build();
    }

    private void attachToPackage(HomebrewPackage pkg, UUID raceId) {
        if (!contentItemRepository.existsByHomebrewPackageIdAndContentTypeAndContentId(pkg.getId(), ContentType.RACE, raceId)) {
            contentItemRepository.save(HomebrewContentItem.builder()
                    .homebrewPackage(pkg)
                    .contentType(ContentType.RACE)
                    .contentId(raceId)
                    .build());
        }
    }

    private HomebrewPackage getEditablePackage(UUID packageId, User gm) {
        HomebrewPackage pkg = homebrewPackageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Homebrew package not found"));
        if (gm.getRole() != Role.ADMIN && !pkg.getAuthor().getId().equals(gm.getId())) {
            throw new AccessDeniedException("Cannot manage races in another user's homebrew package");
        }
        if (pkg.isDeleted() || pkg.getStatus() != HomebrewStatus.DRAFT) {
            throw new DuplicateResourceException("Homebrew race content can be changed only in DRAFT package");
        }
        return pkg;
    }

    private void requireSamePackage(HomebrewPackage pkg, CharacterRace race) {
        if (race.getHomebrew() == null || !race.getHomebrew().getId().equals(pkg.getId())) {
            throw new AccessDeniedException("Race does not belong to this homebrew package");
        }
    }

    private User requireGameMaster(String username) {
        User user = getUser(username);
        if (user.getRole() != Role.GAME_MASTER && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only game masters can manage homebrew races");
        }
        return user;
    }

    private User requireRole(String username, Role role) {
        User user = getUser(username);
        if (user.getRole() != role) {
            throw new AccessDeniedException("Required role: " + role);
        }
        return user;
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private CharacterRace getRaceOrThrow(UUID raceId) {
        return raceRepository.findById(raceId)
                .orElseThrow(() -> new ResourceNotFoundException("Race not found"));
    }

    private String nextCopyName(String sourceName, String packageTitle) {
        String base = sourceName + " - " + packageTitle;
        if (!raceRepository.existsByName(base)) {
            return base;
        }
        int suffix = 2;
        while (raceRepository.existsByName(base + " " + suffix)) {
            suffix++;
        }
        return base + " " + suffix;
    }

    private List<RaceTraitRequest> withGeneratedTraitIds(List<RaceTraitRequest> traits) {
        if (traits == null) return List.of();
        traits.forEach(t -> {
            if (t.getId() == null) t.setId(UUID.randomUUID());
        });
        return traits;
    }

    private List<RaceLineageRequest> withGeneratedLineageIds(List<RaceLineageRequest> lineages) {
        if (lineages == null) return List.of();
        lineages.forEach(lineage -> {
            if (lineage.getId() == null) lineage.setId(UUID.randomUUID());
            withGeneratedTraitIds(lineage.getTraits());
        });
        return lineages;
    }

    private <T> List<T> withDefaultList(List<T> list) {
        return list != null ? list : List.of();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " is required");
        }
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid " + fieldName + ": " + value);
        }
    }

    private String write(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid race JSON payload: " + e.getMessage());
        }
    }

    private <T> T read(String json, TypeReference<T> type, T fallback) {
        if (json == null || json.isBlank()) return fallback;
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            return fallback;
        }
    }

    private JsonNode readJsonNode(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
