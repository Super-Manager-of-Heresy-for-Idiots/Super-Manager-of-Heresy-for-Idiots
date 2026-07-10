package com.dnd.app.service;

import com.dnd.app.domain.content.Species;
import com.dnd.app.domain.content.SpeciesSpeed;
import com.dnd.app.domain.content.SpeciesTrait;
import com.dnd.app.domain.content.SpeciesTraitEffect;
import com.dnd.app.dto.request.RaceSpeedDto;
import com.dnd.app.dto.response.CharacterRaceSnapshotResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CampaignHomebrewRepository;
import com.dnd.app.repository.SpeciesRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Set;
import java.util.UUID;

/**
 * Класс SpeciesService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeciesService {

    private final SpeciesRepository speciesRepository;
    private final CampaignHomebrewRepository campaignHomebrewRepository;
    private final ObjectMapper objectMapper;

    /**
     * Возвращает результат операции "get selectable species" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param speciesId идентификатор species, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public Species getSelectableSpecies(UUID campaignId, UUID speciesId) {
        Species species = getOrThrow(speciesId);
        if (species.getHomebrew() == null) {
            return species;
        }
        Set<UUID> packageIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
        if (!packageIds.contains(species.getHomebrew().getId())) {
            throw new BadRequestException("Selected species is not available in this campaign");
        }
        return species;
    }

    /**
     * Возвращает результат операции "get selectable vanilla species" в рамках бизнес-логики домена.
     * @param speciesId идентификатор species, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public Species getSelectableVanillaSpecies(UUID speciesId) {
        Species species = getOrThrow(speciesId);
        if (species.getHomebrew() != null) {
            throw new BadRequestException("Homebrew species cannot be used in vanilla characters");
        }
        return species;
    }

    /**
     * Формирует результат операции "build species snapshot json" в рамках бизнес-логики домена.
     * @param species входящее значение species, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public String buildSpeciesSnapshotJson(Species species) {
        CharacterRaceSnapshotResponse snapshot = CharacterRaceSnapshotResponse.builder()
                .raceId(species.getId())
                .raceName(species.getNameEn() != null ? species.getNameEn() : species.getNameRu())
                .lineageId(null)   // no lineages/subraces in the 2024 model
                .lineageName(null)
                .size(defaultSizeSlug(species))
                .speed(buildSpeed(species))
                .darkvisionRange(darkvisionRange(species))
                .traitNames(species.getTraits().stream()
                        .sorted(Comparator.comparing(SpeciesTrait::getSortOrder,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(SpeciesTrait::getName)
                        .toList())
                .allowAbilityScoreBonuses(false) // ASI comes from Background in 2024
                .build();
        return write(snapshot);
    }

    /**
     * Выполняет операции "parse snapshot" в рамках бизнес-логики домена.
     * @param snapshotJson входящее значение snapshot json, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public CharacterRaceSnapshotResponse parseSnapshot(String snapshotJson) {
        if (snapshotJson == null || snapshotJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(snapshotJson, new TypeReference<CharacterRaceSnapshotResponse>() {});
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid species snapshot payload: " + e.getMessage(), e);
        }
    }

    private String defaultSizeSlug(Species species) {
        return species.getSizeOptions().stream()
                .map(com.dnd.app.domain.CreatureSize::getSlug)
                .sorted(Comparator.nullsLast(Comparator.naturalOrder()))
                .findFirst()
                .orElse(null);
    }

    private RaceSpeedDto buildSpeed(Species species) {
        RaceSpeedDto.RaceSpeedDtoBuilder b = RaceSpeedDto.builder();
        for (SpeciesSpeed sp : species.getSpeeds()) {
            String type = sp.getSpeedTypeSlug() != null ? sp.getSpeedTypeSlug().toLowerCase() : "";
            Integer amt = sp.getAmountFt();
            switch (type) {
                case "walk" -> b.walk(amt);
                case "fly" -> b.fly(amt);
                case "swim" -> b.swim(amt);
                case "climb" -> b.climb(amt);
                case "burrow" -> b.burrow(amt);
                default -> { /* unknown speed type: ignored in the legacy snapshot shape */ }
            }
        }
        return b.build();
    }

    private Integer darkvisionRange(Species species) {
        return species.getTraits().stream()
                .flatMap(t -> t.getEffects().stream())
                .filter(e -> e.getEffectType() != null
                        && e.getEffectType().toLowerCase().contains("darkvision"))
                .map(SpeciesTraitEffect::getRangeFt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    private Species getOrThrow(UUID speciesId) {
        return speciesRepository.findById(speciesId)
                .orElseThrow(() -> new ResourceNotFoundException("Species not found"));
    }

    private String write(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid species snapshot payload: " + e.getMessage(), e);
        }
    }
}
