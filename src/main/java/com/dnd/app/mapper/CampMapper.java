package com.dnd.app.mapper;

import com.dnd.app.domain.CampActivity;
import com.dnd.app.domain.CampEvent;
import com.dnd.app.domain.CampParticipant;
import com.dnd.app.domain.CampSession;
import com.dnd.app.domain.CampaignLocation;
import com.dnd.app.domain.CharacterClassLevel;
import com.dnd.app.domain.CharacterHitDie;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.enums.CampActivityKind;
import com.dnd.app.domain.enums.CampStatus;
import com.dnd.app.dto.camp.CampWatchSlotDefinition;
import com.dnd.app.dto.response.CampActivityResponse;
import com.dnd.app.dto.response.CampEventResponse;
import com.dnd.app.dto.response.CampParticipantResponse;
import com.dnd.app.dto.response.CampSessionResponse;
import com.dnd.app.dto.response.CampWatchSlotResponse;
import com.dnd.app.dto.response.ClassLevelResponse;
import com.dnd.app.dto.response.HitDiceResponse;
import com.dnd.app.dto.response.LocationRefResponse;
import com.dnd.app.dto.response.RestResult;
import com.dnd.app.repository.CharacterClassLevelRepository;
import com.dnd.app.repository.CharacterHitDieRepository;
import com.dnd.app.util.CampStatusRules;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Класс CampMapper описывает маппер, который собирает состояние привала в ответ API
 * без изменения бизнес-правил: расписание дозора, состав с хитами и костями хитов,
 * журнал событий и допустимые переходы статуса.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CampMapper {

    private final CharacterHitDieRepository hitDieRepository;
    private final CharacterClassLevelRepository classLevelRepository;
    private final ObjectMapper objectMapper;

    /**
     * Собирает полное состояние привала для экрана лагеря.
     * @param camp привал
     * @param participants участники привала
     * @param events записи журнала, уже отфильтрованные по видимости для запрашивающего
     * @return состояние привала
     */
    public CampSessionResponse toResponse(CampSession camp, List<CampParticipant> participants,
                                          List<CampEvent> events) {
        List<CampParticipantResponse> participantResponses = participants.stream()
                .map(this::toParticipantResponse)
                .toList();

        return CampSessionResponse.builder()
                .id(camp.getId())
                .campaignId(camp.getCampaign().getId())
                .name(camp.getName())
                .description(camp.getDescription())
                .dayNumber(camp.getDayNumber())
                .status(camp.getStatus().name())
                .visitedStatuses(visitedStatuses(camp))
                .availableTransitions(CampStatusRules.allowedTransitions(camp.getStatus())
                        .stream().map(Enum::name).toList())
                .restType(camp.getRestType())
                .applyPartialRest(camp.getApplyPartialRest())
                .location(toLocationRef(camp.getLocation()))
                .restSafety(camp.getLocation() != null && camp.getLocation().getRestSafety() != null
                        ? camp.getLocation().getRestSafety().name() : null)
                .interruptReason(camp.getInterruptReason() != null ? camp.getInterruptReason().name() : null)
                .interruptEventId(camp.getInterruptEvent() != null ? camp.getInterruptEvent().getId() : null)
                .interruptBattleId(camp.getInterruptEvent() != null && camp.getInterruptEvent().getBattle() != null
                        ? camp.getInterruptEvent().getBattle().getId() : null)
                .watchSlotCount(camp.getWatchSlotCount())
                .watchSchedule(buildWatchSchedule(camp, participants))
                .participants(participantResponses)
                .events(events.stream().map(this::toEventResponse).toList())
                .createdById(camp.getCreatedBy().getId())
                .createdByUsername(camp.getCreatedBy().getUsername())
                .startedAt(camp.getStartedAt())
                .restStartedAt(camp.getRestStartedAt())
                .restCompletedAt(camp.getRestCompletedAt())
                .interruptedAt(camp.getInterruptedAt())
                .endedAt(camp.getEndedAt())
                .createdAt(camp.getCreatedAt())
                .updatedAt(camp.getUpdatedAt())
                .build();
    }

    /**
     * Собирает карточку участника привала: состояние отдыха, хиты, кости хитов, дозор и активность.
     * @param participant участник привала
     * @return карточка участника
     */
    public CampParticipantResponse toParticipantResponse(CampParticipant participant) {
        PlayerCharacter character = participant.getCharacter();
        CampActivity activity = participant.getActivity();
        return CampParticipantResponse.builder()
                .id(participant.getId())
                .characterId(character.getId())
                .characterName(character.getName())
                .ownerId(character.getOwner() != null ? character.getOwner().getId() : null)
                .ownerUsername(character.getOwner() != null ? character.getOwner().getUsername() : null)
                .avatarUrl(character.getAvatarUrl())
                .totalLevel(character.getTotalLevel())
                .classLevels(toClassLevels(character.getId()))
                .state(participant.getState().name())
                .currentHp(character.getCurrentHp())
                .maxHp(character.getMaxHp())
                .tempHp(character.getTempHp())
                .hitDice(toHitDice(character.getId()))
                .watchSlot(participant.getWatchSlot())
                .watchSlotLabel(watchSlotLabel(participant))
                .activityId(activity != null ? activity.getId() : null)
                .activityName(activity != null ? activity.getName() : null)
                .activityIconCode(activity != null ? activity.getIconCode() : null)
                .activityNote(participant.getActivityNote())
                .restResult(readRestResult(participant))
                .restErrorCode(participant.getRestErrorCode())
                .restErrorMessage(participant.getRestErrorMessage())
                .restedAt(participant.getRestedAt())
                .build();
    }

    /**
     * Преобразует запись журнала привала в ответ API.
     * @param event запись журнала
     * @return запись журнала для клиента
     */
    public CampEventResponse toEventResponse(CampEvent event) {
        return CampEventResponse.builder()
                .id(event.getId())
                .type(event.getType().name())
                .title(event.getTitle())
                .description(event.getDescription())
                .occurredLabel(event.getOccurredLabel())
                .visibleToPlayers(event.getVisibleToPlayers())
                .battleId(event.getBattle() != null ? event.getBattle().getId() : null)
                .triggeredAt(event.getTriggeredAt())
                .createdAt(event.getCreatedAt())
                .createdByUsername(event.getCreatedBy() != null ? event.getCreatedBy().getUsername() : null)
                .build();
    }

    /**
     * Преобразует даунтайм-активность в ответ API; вид (системная/кастомная) выводится
     * из принадлежности кампании.
     * @param activity активность справочника
     * @return активность для клиента
     */
    public CampActivityResponse toActivityResponse(CampActivity activity) {
        boolean system = activity.getCampaign() == null;
        return CampActivityResponse.builder()
                .id(activity.getId())
                .campaignId(system ? null : activity.getCampaign().getId())
                .kind((system ? CampActivityKind.SYSTEM : CampActivityKind.CUSTOM).name())
                .name(activity.getName())
                .description(activity.getDescription())
                .iconCode(activity.getIconCode())
                .skillCheckRef(activity.getSkillCheckRef())
                .build();
    }

    /**
     * Читает расписание дозора привала из хранимого JSON.
     * @param camp привал
     * @return определения слотов; пустой список, если расписание не задано или повреждено
     */
    public List<CampWatchSlotDefinition> readWatchSchedule(CampSession camp) {
        String json = camp.getWatchScheduleJson();
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<CampWatchSlotDefinition>>() { });
        } catch (Exception e) {
            log.warn("Camp watch schedule is unreadable: campId={}, reason={}", camp.getId(), e.getMessage());
            return List.of();
        }
    }

    private List<CampWatchSlotResponse> buildWatchSchedule(CampSession camp, List<CampParticipant> participants) {
        Map<Integer, CampParticipant> bySlot = new HashMap<>();
        for (CampParticipant participant : participants) {
            if (participant.getWatchSlot() != null) {
                bySlot.put(participant.getWatchSlot(), participant);
            }
        }

        Map<Integer, String> labels = new HashMap<>();
        for (CampWatchSlotDefinition definition : readWatchSchedule(camp)) {
            if (definition.slot() != null) {
                labels.put(definition.slot(), definition.label());
            }
        }

        int slotCount = camp.getWatchSlotCount() != null ? camp.getWatchSlotCount() : 0;
        List<CampWatchSlotResponse> schedule = new ArrayList<>();
        for (int slot = 1; slot <= slotCount; slot++) {
            CampParticipant assigned = bySlot.get(slot);
            schedule.add(CampWatchSlotResponse.builder()
                    .slot(slot)
                    .label(labels.get(slot))
                    .characterId(assigned != null ? assigned.getCharacter().getId() : null)
                    .characterName(assigned != null ? assigned.getCharacter().getName() : null)
                    .build());
        }
        return schedule;
    }

    private String watchSlotLabel(CampParticipant participant) {
        if (participant.getWatchSlot() == null) {
            return null;
        }
        // findFirst до map: метка слота может быть null, а Stream.findFirst()
        // на null-элементе бросает NPE; Optional.map переносит null безопасно.
        return readWatchSchedule(participant.getCampSession()).stream()
                .filter(definition -> participant.getWatchSlot().equals(definition.slot()))
                .findFirst()
                .map(CampWatchSlotDefinition::label)
                .orElse(null);
    }

    private List<String> visitedStatuses(CampSession camp) {
        List<String> visited = new ArrayList<>();
        visited.add(CampStatus.SETTING_UP.name());
        if (camp.getStartedAt() != null) {
            visited.add(CampStatus.ACTIVE.name());
        }
        if (camp.getRestStartedAt() != null) {
            visited.add(CampStatus.RESTING.name());
        }
        if (camp.getInterruptedAt() != null) {
            visited.add(CampStatus.INTERRUPTED.name());
        }
        if (camp.getEndedAt() != null) {
            visited.add(CampStatus.COMPLETED.name());
        }
        return visited;
    }

    private LocationRefResponse toLocationRef(CampaignLocation location) {
        if (location == null) {
            return null;
        }
        return LocationRefResponse.builder()
                .id(location.getId())
                .name(location.getName())
                .build();
    }

    private List<ClassLevelResponse> toClassLevels(UUID characterId) {
        return classLevelRepository.findAllByCharacterId(characterId).stream()
                .map(this::toClassLevel)
                .toList();
    }

    private ClassLevelResponse toClassLevel(CharacterClassLevel classLevel) {
        return ClassLevelResponse.builder()
                .classId(classLevel.getClassId())
                .className(classLevel.getCharacterClass() != null
                        ? classLevel.getCharacterClass().getNameRu() : null)
                .classLevel(classLevel.getClassLevel())
                .build();
    }

    private List<HitDiceResponse> toHitDice(UUID characterId) {
        return hitDieRepository.findByCharacterId(characterId).stream()
                .sorted(Comparator.comparingInt(CharacterHitDie::getDie).reversed())
                .map(die -> HitDiceResponse.builder()
                        .die(die.getDie())
                        .total(die.getTotal())
                        .remaining(die.getRemaining())
                        .build())
                .toList();
    }

    private RestResult readRestResult(CampParticipant participant) {
        String json = participant.getRestResultJson();
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, RestResult.class);
        } catch (Exception e) {
            log.warn("Camp rest result snapshot is unreadable: participantId={}, reason={}",
                    participant.getId(), e.getMessage());
            return null;
        }
    }
}
