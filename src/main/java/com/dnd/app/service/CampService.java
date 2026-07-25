package com.dnd.app.service;

import com.dnd.app.domain.CampEvent;
import com.dnd.app.domain.CampParticipant;
import com.dnd.app.domain.CampSession;
import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.CampaignLocation;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.CampInterruptReason;
import com.dnd.app.domain.enums.CampParticipantState;
import com.dnd.app.domain.enums.CampStatus;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.camp.CampWatchSlotDefinition;
import com.dnd.app.dto.request.CampWatchSlotRequest;
import com.dnd.app.dto.request.CreateCampRequest;
import com.dnd.app.dto.request.InterruptCampRequest;
import com.dnd.app.dto.request.UpdateCampRequest;
import com.dnd.app.dto.request.UpdateCampWatchOrderRequest;
import com.dnd.app.dto.response.CampSessionResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.mapper.CampMapper;
import com.dnd.app.repository.CampEventRepository;
import com.dnd.app.repository.CampParticipantRepository;
import com.dnd.app.repository.CampSessionRepository;
import com.dnd.app.repository.CampaignLocationRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.util.CampStatusRules;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Класс CampService описывает жизненный цикл привала: разбивку лагеря, состав, дозор,
 * переходы state-machine и прерывание. Сам отдых сервис не считает — его применяет
 * {@link CampRestService} поверх {@link RestOrchestrationService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampService {

    private final CampSessionRepository campSessionRepository;
    private final CampParticipantRepository campParticipantRepository;
    private final CampEventRepository campEventRepository;
    private final CampaignLocationRepository locationRepository;
    private final PlayerCharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final WebSocketEventService webSocketEventService;
    private final CampMapper campMapper;
    private final ObjectMapper objectMapper;

    /**
     * Разбивает лагерь: создаёт привал в статусе SETTING_UP с составом и первичным дозором.
     * @param campaignId идентификатор кампании
     * @param request параметры привала
     * @param username имя пользователя-мастера
     * @return состояние созданного привала
     */
    @Transactional
    public CampSessionResponse createCamp(UUID campaignId, CreateCampRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        campaignService.enforceCampaignActive(campaign);

        campSessionRepository.findCurrent(campaignId, CampStatus.COMPLETED).ifPresent(existing -> {
            throw new BadRequestException("В кампании уже есть незавершённый привал");
        });

        CampSession camp = CampSession.builder()
                .campaign(campaign)
                .location(resolveLocation(campaignId, request.getLocationId()))
                .name(request.getName())
                .description(request.getDescription())
                .dayNumber(request.getDayNumber())
                .status(CampStatus.SETTING_UP)
                .createdBy(user)
                .build();
        applyWatchSchedule(camp, request.getWatchSlotCount(), request.getWatchSchedule());
        try {
            // Проверка выше не атомарна: инвариант "один незавершённый привал" держит
            // частичный уникальный индекс, и параллельная разбивка лагеря должна
            // получить осмысленный 400, а не 500 от нарушения ограничения.
            camp = campSessionRepository.saveAndFlush(camp);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("В кампании уже есть незавершённый привал");
        }

        List<CampParticipant> participants = replaceParticipants(camp, campaignId,
                request.getParticipantCharacterIds());
        assignWatchSlots(camp, participants, request.getWatchSchedule());

        log.info("Camp created: campaignId={}, campId={}, participants={}, by={}",
                campaignId, camp.getId(), participants.size(), username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.CAMP_STARTED, campaignId,
                Map.of("campId", camp.getId(), "status", camp.getStatus().name()), user.getId());
        return toResponse(camp, user);
    }

    /**
     * Возвращает текущий незавершённый привал кампании.
     * @param campaignId идентификатор кампании
     * @param username имя пользователя, выполняющего запрос
     * @return состояние привала или null, если привал не разбит
     */
    @Transactional(readOnly = true)
    public CampSessionResponse getCurrentCamp(UUID campaignId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);

        return campSessionRepository.findCurrent(campaignId, CampStatus.COMPLETED)
                .map(camp -> toResponse(camp, user))
                .orElse(null);
    }

    /**
     * Возвращает конкретный привал кампании, включая завершённые (архив).
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param username имя пользователя, выполняющего запрос
     * @return состояние привала
     */
    @Transactional(readOnly = true)
    public CampSessionResponse getCamp(UUID campaignId, UUID campId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        return toResponse(findCamp(campaignId, campId), user);
    }

    /**
     * Возвращает историю привалов кампании (свежие первыми).
     * @param campaignId идентификатор кампании
     * @param username имя пользователя, выполняющего запрос
     * @return список привалов
     */
    @Transactional(readOnly = true)
    public List<CampSessionResponse> listCamps(UUID campaignId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);

        return campSessionRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId).stream()
                .map(camp -> toResponse(camp, user))
                .toList();
    }

    /**
     * Правит привал: название, описание, день, локацию и состав участников.
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param request изменяемые поля
     * @param username имя пользователя-мастера
     * @return обновлённое состояние привала
     */
    @Transactional
    public CampSessionResponse updateCamp(UUID campaignId, UUID campId, UpdateCampRequest request, String username) {
        User user = requireGmForChange(campaignId, username);
        CampSession camp = findCampForUpdate(campaignId, campId);
        enforceNotCompleted(camp);

        if (request.getName() != null && !request.getName().isBlank()) {
            camp.setName(request.getName());
        }
        if (request.getDescription() != null) {
            camp.setDescription(request.getDescription());
        }
        if (request.getDayNumber() != null) {
            camp.setDayNumber(request.getDayNumber());
        }
        if (Boolean.TRUE.equals(request.getClearLocation())) {
            camp.setLocation(null);
        } else if (request.getLocationId() != null) {
            camp.setLocation(resolveLocation(campaignId, request.getLocationId()));
        }
        if (request.getParticipantCharacterIds() != null) {
            replaceParticipants(camp, campaignId, request.getParticipantCharacterIds());
        }
        campSessionRepository.save(camp);

        log.info("Camp updated: campId={}, by={}", campId, username);
        publishCampUpdate(WebSocketEventType.CAMP_UPDATED, camp, user);
        return toResponse(camp, user);
    }

    /**
     * Перестраивает дозор целиком: слоты вне переданного расписания остаются пустыми.
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param request новое расписание дозора
     * @param username имя пользователя-мастера
     * @return обновлённое состояние привала
     */
    @Transactional
    public CampSessionResponse updateWatchOrder(UUID campaignId, UUID campId,
                                                UpdateCampWatchOrderRequest request, String username) {
        User user = requireGmForChange(campaignId, username);
        CampSession camp = findCampForUpdate(campaignId, campId);
        enforceNotCompleted(camp);

        applyWatchSchedule(camp, request.getWatchSlotCount(), request.getWatchSchedule());
        campSessionRepository.save(camp);
        assignWatchSlots(camp, campParticipantRepository.findByCampSessionIdOrderByCreatedAtAsc(campId),
                request.getWatchSchedule());

        log.info("Camp watch order updated: campId={}, slots={}, by={}", campId, camp.getWatchSlotCount(), username);
        publishCampUpdate(WebSocketEventType.CAMP_UPDATED, camp, user);
        return toResponse(camp, user);
    }

    /**
     * Начинает привал: SETTING_UP или INTERRUPTED переводятся в ACTIVE.
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param username имя пользователя-мастера
     * @return обновлённое состояние привала
     */
    @Transactional
    public CampSessionResponse startCamp(UUID campaignId, UUID campId, String username) {
        User user = requireGmForChange(campaignId, username);
        CampSession camp = findCampForUpdate(campaignId, campId);
        enforceTransition(camp, CampStatus.ACTIVE);

        boolean returning = camp.getStatus() == CampStatus.INTERRUPTED;
        camp.setStatus(CampStatus.ACTIVE);
        if (camp.getStartedAt() == null) {
            camp.setStartedAt(Instant.now());
        }
        if (returning) {
            camp.setInterruptReason(null);
            camp.setInterruptEvent(null);
            camp.setRestType(null);
        }
        campSessionRepository.save(camp);

        log.info("Camp activated: campId={}, returning={}, by={}", campId, returning, username);
        publishCampUpdate(WebSocketEventType.CAMP_UPDATED, camp, user);
        return toResponse(camp, user);
    }

    /**
     * Прерывает привал: переводит в INTERRUPTED и фиксирует причину. Восстановление
     * по умолчанию не применяется — частичный отдых засчитывается отдельным решением мастера.
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param request причина прерывания и связанное событие журнала
     * @param username имя пользователя-мастера
     * @return обновлённое состояние привала
     */
    @Transactional
    public CampSessionResponse interruptCamp(UUID campaignId, UUID campId,
                                             InterruptCampRequest request, String username) {
        User user = requireGm(campaignId, username);
        CampSession camp = findCampForUpdate(campaignId, campId);
        enforceTransition(camp, CampStatus.INTERRUPTED);

        CampInterruptReason reason = request != null && request.getReason() != null
                ? request.getReason() : CampInterruptReason.MANUAL;
        CampEvent event = null;
        if (request != null && request.getEventId() != null) {
            event = campEventRepository.findByIdAndCampSessionId(request.getEventId(), campId)
                    .orElseThrow(() -> new ResourceNotFoundException("Событие привала не найдено"));
        }

        applyInterruption(camp, reason, event, user);

        log.info("Camp interrupted: campId={}, reason={}, by={}", campId, reason, username);
        return toResponse(camp, user);
    }

    /**
     * Переводит привал в INTERRUPTED: фиксирует причину, снимает участников с незавершённого
     * отдыха и рассылает событие. Единая точка для ручного прерывания мастером и для засады
     * из журнала — иначе состояния участников расходились бы между двумя путями.
     * @param camp привал
     * @param reason причина прерывания
     * @param event событие журнала — причина прерывания (может быть null)
     * @param user пользователь, инициировавший прерывание
     */
    @Transactional
    public void applyInterruption(CampSession camp, CampInterruptReason reason,
                                  CampEvent event, User user) {
        camp.setStatus(CampStatus.INTERRUPTED);
        camp.setInterruptReason(reason);
        camp.setInterruptEvent(event);
        camp.setInterruptedAt(Instant.now());
        campSessionRepository.save(camp);

        // Прерванный отдых не считается применённым: участники возвращаются к "не отдохнул",
        // пока мастер отдельным решением не засчитает частичный отдых.
        for (CampParticipant participant : campParticipantRepository
                .findByCampSessionIdOrderByCreatedAtAsc(camp.getId())) {
            if (participant.getState() == CampParticipantState.RESTING) {
                participant.setState(CampParticipantState.NOT_RESTED);
                campParticipantRepository.save(participant);
            }
        }

        webSocketEventService.sendCampaignEvent(WebSocketEventType.CAMP_INTERRUPTED,
                camp.getCampaign().getId(),
                Map.of("campId", camp.getId(), "reason", reason.name()), user.getId());
    }

    /**
     * Сворачивает лагерь: переводит привал в терминальный статус COMPLETED.
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param username имя пользователя-мастера
     * @return обновлённое состояние привала
     */
    @Transactional
    public CampSessionResponse endCamp(UUID campaignId, UUID campId, String username) {
        User user = requireGm(campaignId, username);
        CampSession camp = findCampForUpdate(campaignId, campId);
        enforceTransition(camp, CampStatus.COMPLETED);

        camp.setStatus(CampStatus.COMPLETED);
        camp.setEndedAt(Instant.now());
        campSessionRepository.save(camp);

        log.info("Camp completed: campId={}, by={}", campId, username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.CAMP_ENDED, campaignId,
                Map.of("campId", camp.getId()), user.getId());
        return toResponse(camp, user);
    }

    // --- Внутренний контракт для CampRestService ---

    /**
     * Загружает привал кампании под блокировкой строки.
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @return привал
     */
    @Transactional
    public CampSession findCampForUpdate(UUID campaignId, UUID campId) {
        return campSessionRepository.findByIdAndCampaignIdForUpdate(campId, campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Привал не найден"));
    }

    /**
     * Загружает привал кампании без блокировки.
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @return привал
     */
    @Transactional(readOnly = true)
    public CampSession findCamp(UUID campaignId, UUID campId) {
        return campSessionRepository.findByIdAndCampaignId(campId, campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Привал не найден"));
    }

    /**
     * Проверяет права мастера кампании и возвращает пользователя. Активность кампании
     * здесь не требуется: этим методом пользуются в том числе пути свёртывания привала,
     * которые должны работать и в приостановленной кампании.
     * @param campaignId идентификатор кампании
     * @param username имя пользователя
     * @return пользователь-мастер
     */
    public User requireGm(UUID campaignId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        return user;
    }

    /**
     * Проверяет права мастера и активность кампании — для действий, продвигающих привал
     * вперёд (отдых, события, активности). В паузе кампании доступны только прерывание
     * и завершение привала.
     * @param campaignId идентификатор кампании
     * @param username имя пользователя
     * @return пользователь-мастер
     */
    public User requireGmForChange(UUID campaignId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        campaignService.enforceCampaignActive(campaign);
        return user;
    }

    /**
     * Собирает состояние привала с учётом видимости журнала для запрашивающего.
     * @param camp привал
     * @param user пользователь, для которого собирается ответ
     * @return состояние привала
     */
    public CampSessionResponse toResponse(CampSession camp, User user) {
        UUID campaignId = camp.getCampaign().getId();
        boolean gm = user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId());
        List<CampEvent> events = gm
                ? campEventRepository.findByCampSessionIdOrderByCreatedAtAsc(camp.getId())
                : campEventRepository.findByCampSessionIdAndVisibleToPlayersTrueOrderByCreatedAtAsc(camp.getId());
        return campMapper.toResponse(camp,
                campParticipantRepository.findByCampSessionIdOrderByCreatedAtAsc(camp.getId()), events);
    }

    /**
     * Публикует состояние привала подписчикам кампании.
     * @param type тип события
     * @param camp привал
     * @param user пользователь, инициировавший изменение
     */
    public void publishCampUpdate(WebSocketEventType type, CampSession camp, User user) {
        webSocketEventService.sendCampaignEvent(type, camp.getCampaign().getId(),
                Map.of("campId", camp.getId(), "status", camp.getStatus().name()), user.getId());
    }

    /**
     * Проверяет, что привал ещё не завершён.
     * @param camp привал
     */
    public void enforceNotCompleted(CampSession camp) {
        if (camp.getStatus() == CampStatus.COMPLETED) {
            throw new BadRequestException("Привал завершён — изменения недоступны");
        }
    }

    // --- Private helpers ---

    private void enforceTransition(CampSession camp, CampStatus target) {
        if (!CampStatusRules.canTransition(camp.getStatus(), target)) {
            throw new BadRequestException("Недопустимый переход привала: "
                    + camp.getStatus() + " → " + target);
        }
    }

    private CampaignLocation resolveLocation(UUID campaignId, UUID locationId) {
        if (locationId == null) {
            return null;
        }
        CampaignLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Локация не найдена"));
        if (location.getCampaign() == null || !location.getCampaign().getId().equals(campaignId)) {
            throw new BadRequestException("Локация принадлежит другой кампании");
        }
        return location;
    }

    private List<CampParticipant> replaceParticipants(CampSession camp, UUID campaignId, List<UUID> characterIds) {
        Set<UUID> requested = characterIds == null ? Set.of() : new LinkedHashSet<>(characterIds);
        List<CampParticipant> existing = campParticipantRepository
                .findByCampSessionIdOrderByCreatedAtAsc(camp.getId());

        for (CampParticipant participant : existing) {
            if (!requested.contains(participant.getCharacter().getId())) {
                campParticipantRepository.delete(participant);
            }
        }

        Set<UUID> present = new HashSet<>();
        for (CampParticipant participant : existing) {
            if (requested.contains(participant.getCharacter().getId())) {
                present.add(participant.getCharacter().getId());
            }
        }

        List<CampParticipant> result = new ArrayList<>();
        for (UUID characterId : requested) {
            if (present.contains(characterId)) {
                campParticipantRepository.findByCampSessionIdAndCharacterId(camp.getId(), characterId)
                        .ifPresent(result::add);
                continue;
            }
            PlayerCharacter character = characterRepository.findById(characterId)
                    .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден: " + characterId));
            if (character.getCampaign() == null || !character.getCampaign().getId().equals(campaignId)) {
                throw new BadRequestException("Персонаж не принадлежит этой кампании: " + characterId);
            }
            result.add(campParticipantRepository.save(CampParticipant.builder()
                    .campSession(camp)
                    .character(character)
                    .state(CampParticipantState.NOT_RESTED)
                    .build()));
        }
        return result;
    }

    private void applyWatchSchedule(CampSession camp, Integer slotCount, List<CampWatchSlotRequest> schedule) {
        int resolvedCount = slotCount != null
                ? slotCount
                : (schedule != null && !schedule.isEmpty()
                        ? schedule.stream().mapToInt(CampWatchSlotRequest::getSlot).max().orElse(0)
                        : (camp.getWatchSlotCount() != null ? camp.getWatchSlotCount() : 0));
        camp.setWatchSlotCount(resolvedCount);

        if (schedule == null) {
            return;
        }
        List<CampWatchSlotDefinition> definitions = schedule.stream()
                .filter(slot -> slot.getSlot() != null && slot.getSlot() <= resolvedCount)
                .map(slot -> new CampWatchSlotDefinition(slot.getSlot(), slot.getLabel()))
                .toList();
        try {
            camp.setWatchScheduleJson(objectMapper.writeValueAsString(definitions));
        } catch (Exception e) {
            throw new BadRequestException("Не удалось сохранить расписание дозора: " + e.getMessage());
        }
    }

    /**
     * Расставляет дозор. Пустое расписание назначения не трогает — только снимает слоты,
     * выпавшие за пределы уменьшенного {@code watchSlotCount}.
     *
     * <p>Снятие и назначение разделены явным flush: частичный уникальный индекс
     * {@code uq_campparticipants_session_watch_slot} проверяется на каждом UPDATE, поэтому
     * освобождение слота обязано попасть в базу раньше, чем его займёт другой персонаж.
     */
    private void assignWatchSlots(CampSession camp, List<CampParticipant> participants,
                                  List<CampWatchSlotRequest> schedule) {
        int slotCount = camp.getWatchSlotCount() != null ? camp.getWatchSlotCount() : 0;

        if (schedule == null) {
            pruneWatchSlots(participants, slotCount);
            return;
        }
        validateWatchSchedule(schedule, slotCount);

        // Фаза 1: освобождаем занятые слоты и доводим изменения до базы.
        boolean cleared = false;
        for (CampParticipant participant : participants) {
            if (participant.getWatchSlot() != null) {
                participant.setWatchSlot(null);
                campParticipantRepository.save(participant);
                cleared = true;
            }
        }
        if (cleared) {
            campParticipantRepository.flush();
        }

        // Фаза 2: занимаем слоты заново — конфликтов с прежними назначениями уже нет.
        for (CampWatchSlotRequest slot : schedule) {
            if (slot.getCharacterId() == null || slot.getSlot() == null || slot.getSlot() > slotCount) {
                continue;
            }
            CampParticipant participant = participants.stream()
                    .filter(p -> p.getCharacter().getId().equals(slot.getCharacterId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException(
                            "В дозор назначен персонаж вне состава привала: " + slot.getCharacterId()));
            participant.setWatchSlot(slot.getSlot());
            campParticipantRepository.save(participant);
        }
    }

    /** Снимает с дозора участников, чьи слоты выпали за пределы расписания. */
    private void pruneWatchSlots(List<CampParticipant> participants, int slotCount) {
        for (CampParticipant participant : participants) {
            if (participant.getWatchSlot() != null && participant.getWatchSlot() > slotCount) {
                participant.setWatchSlot(null);
                campParticipantRepository.save(participant);
            }
        }
    }

    /**
     * Проверяет расписание дозора до записи: один слот — один персонаж, один персонаж —
     * один слот. Без этой проверки нарушение уникального индекса всплыло бы как 500.
     */
    private void validateWatchSchedule(List<CampWatchSlotRequest> schedule, int slotCount) {
        Set<Integer> slots = new HashSet<>();
        Set<UUID> characters = new HashSet<>();
        for (CampWatchSlotRequest slot : schedule) {
            if (slot.getSlot() == null) {
                continue;
            }
            if (!slots.add(slot.getSlot())) {
                throw new BadRequestException("Слот дозора указан дважды: " + slot.getSlot());
            }
            if (slot.getCharacterId() != null
                    && slot.getSlot() <= slotCount
                    && !characters.add(slot.getCharacterId())) {
                throw new BadRequestException("Персонаж назначен в несколько слотов дозора: "
                        + slot.getCharacterId());
            }
        }
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }
}
