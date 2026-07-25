package com.dnd.app.service;

import com.dnd.app.domain.Battle;
import com.dnd.app.domain.CampEvent;
import com.dnd.app.domain.CampSession;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.CampEventType;
import com.dnd.app.domain.enums.CampInterruptReason;
import com.dnd.app.domain.enums.CampStatus;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.request.AddBattleMonstersRequest;
import com.dnd.app.dto.request.CreateBattleRequest;
import com.dnd.app.dto.request.CreateCampEventRequest;
import com.dnd.app.dto.response.BattleResponse;
import com.dnd.app.dto.response.CampEventResponse;
import com.dnd.app.dto.response.CampSessionResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.mapper.CampMapper;
import com.dnd.app.repository.BattleRepository;
import com.dnd.app.repository.CampEventRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.util.CampStatusRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Класс CampEventService описывает журнал привала: засады, встречи, сюжет и погоду.
 * Механических автоэффектов у событий нет — система фиксирует факт, награды выдаёт мастер.
 * Исключение одно: из засады мастер может создать бой, и тогда привал прерывается.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampEventService {

    private final CampEventRepository campEventRepository;
    private final BattleRepository battleRepository;
    private final UserRepository userRepository;
    private final CampService campService;
    private final CampaignService campaignService;
    private final BattleService battleService;
    private final WebSocketEventService webSocketEventService;
    private final CampMapper campMapper;

    /**
     * Возвращает журнал привала: мастер видит все записи, игрок — только видимые.
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param username имя пользователя, выполняющего запрос
     * @return записи журнала в хронологическом порядке
     */
    @Transactional(readOnly = true)
    public List<CampEventResponse> listEvents(UUID campaignId, UUID campId, String username) {
        User user = getUser(username);
        campaignService.enforceMembershipOrAdmin(campaignService.findCampaign(campaignId), user);
        campService.findCamp(campaignId, campId);

        boolean gm = user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId());
        List<CampEvent> events = gm
                ? campEventRepository.findByCampSessionIdOrderByCreatedAtAsc(campId)
                : campEventRepository.findByCampSessionIdAndVisibleToPlayersTrueOrderByCreatedAtAsc(campId);
        return events.stream().map(campMapper::toEventResponse).toList();
    }

    /**
     * Записывает событие привала. Незапущенное событие остаётся скрытой заготовкой мастера;
     * засада с созданием боя переводит привал в INTERRUPTED.
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param request параметры события
     * @param username имя пользователя-мастера
     * @return состояние привала после записи события
     */
    @Transactional
    public CampSessionResponse createEvent(UUID campaignId, UUID campId,
                                           CreateCampEventRequest request, String username) {
        User user = campService.requireGmForChange(campaignId, username);
        CampSession camp = campService.findCampForUpdate(campaignId, campId);
        campService.enforceNotCompleted(camp);

        boolean triggerNow = !Boolean.FALSE.equals(request.getTriggerNow());
        boolean ambush = request.getType() == CampEventType.AMBUSH;
        boolean withBattle = ambush && Boolean.TRUE.equals(request.getCreateBattle());

        if (withBattle && (request.getMonsters() == null || request.getMonsters().isEmpty())) {
            throw new BadRequestException("Для боя из засады нужен хотя бы один монстр");
        }

        CampEvent event = CampEvent.builder()
                .campSession(camp)
                .type(request.getType())
                .title(request.getTitle())
                .description(request.getDescription())
                .occurredLabel(request.getOccurredLabel())
                // Заготовка мастера скрыта от игроков, пока не сработает.
                .visibleToPlayers(triggerNow && !Boolean.FALSE.equals(request.getVisibleToPlayers()))
                .triggeredAt(triggerNow ? Instant.now() : null)
                .createdBy(user)
                .build();

        if (withBattle) {
            event.setBattle(createAmbushBattle(campaignId, camp, request, username));
        }
        event = campEventRepository.save(event);

        log.info("Camp event created: campId={}, type={}, triggered={}, battle={}, by={}",
                campId, request.getType(), triggerNow,
                event.getBattle() != null ? event.getBattle().getId() : null, username);

        if (triggerNow) {
            publishTriggered(camp, event, user);
            if (ambush) {
                interruptIfPossible(camp, event, user);
            }
        }
        return campService.toResponse(camp, user);
    }

    /**
     * Триггерит заранее подготовленную заготовку события.
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param eventId идентификатор события
     * @param username имя пользователя-мастера
     * @return состояние привала после срабатывания события
     */
    @Transactional
    public CampSessionResponse triggerEvent(UUID campaignId, UUID campId, UUID eventId, String username) {
        User user = campService.requireGmForChange(campaignId, username);
        CampSession camp = campService.findCampForUpdate(campaignId, campId);
        campService.enforceNotCompleted(camp);

        CampEvent event = campEventRepository.findByIdAndCampSessionId(eventId, campId)
                .orElseThrow(() -> new ResourceNotFoundException("Событие привала не найдено"));
        if (event.getTriggeredAt() != null) {
            throw new BadRequestException("Событие уже сработало");
        }

        event.setTriggeredAt(Instant.now());
        event.setVisibleToPlayers(true);
        campEventRepository.save(event);

        log.info("Camp event triggered: campId={}, eventId={}, type={}, by={}",
                campId, eventId, event.getType(), username);

        publishTriggered(camp, event, user);
        if (event.getType() == CampEventType.AMBUSH) {
            interruptIfPossible(camp, event, user);
        }
        return campService.toResponse(camp, user);
    }

    /**
     * Удаляет запись журнала. Созданный из засады бой при этом не трогается —
     * им мастер распоряжается через обычный интерфейс боя.
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param eventId идентификатор события
     * @param username имя пользователя-мастера
     */
    @Transactional
    public void deleteEvent(UUID campaignId, UUID campId, UUID eventId, String username) {
        campService.requireGmForChange(campaignId, username);
        CampSession camp = campService.findCampForUpdate(campaignId, campId);
        campService.enforceNotCompleted(camp);

        CampEvent event = campEventRepository.findByIdAndCampSessionId(eventId, campId)
                .orElseThrow(() -> new ResourceNotFoundException("Событие привала не найдено"));
        if (camp.getInterruptEvent() != null && camp.getInterruptEvent().getId().equals(eventId)) {
            throw new BadRequestException("Нельзя удалить событие, прервавшее привал");
        }

        campEventRepository.delete(event);
        log.info("Camp event deleted: campId={}, eventId={}, by={}", campId, eventId, username);
    }

    // --- Private helpers ---

    private Battle createAmbushBattle(UUID campaignId, CampSession camp,
                                      CreateCampEventRequest request, String username) {
        String battleName = request.getBattleName() != null && !request.getBattleName().isBlank()
                ? request.getBattleName()
                : request.getTitle();
        BattleResponse battle = battleService.createBattle(campaignId, CreateBattleRequest.builder()
                .name(battleName)
                .locationId(camp.getLocation() != null ? camp.getLocation().getId() : null)
                .build(), username);

        battleService.addMonsters(campaignId, battle.getId(), AddBattleMonstersRequest.builder()
                .monsters(request.getMonsters())
                .build(), username);

        return battleRepository.findByIdAndCampaignId(battle.getId(), campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Созданный бой не найден"));
    }

    /**
     * Прерывает привал засадой, если state-machine это позволяет. Из статуса SETTING_UP
     * прерывать нечего — событие просто остаётся в журнале. Само прерывание выполняет
     * {@link CampService#applyInterruption}, чтобы состояние участников не расходилось
     * с ручным прерыванием мастера.
     */
    private void interruptIfPossible(CampSession camp, CampEvent event, User user) {
        if (!CampStatusRules.canTransition(camp.getStatus(), CampStatus.INTERRUPTED)) {
            return;
        }
        campService.applyInterruption(camp, CampInterruptReason.AMBUSH, event, user);
        log.info("Camp interrupted by ambush: campId={}, eventId={}", camp.getId(), event.getId());
    }

    private void publishTriggered(CampSession camp, CampEvent event, User user) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("campId", camp.getId());
        payload.put("eventId", event.getId());
        payload.put("type", event.getType().name());
        if (event.getBattle() != null) {
            payload.put("battleId", event.getBattle().getId());
        }
        webSocketEventService.sendCampaignEvent(WebSocketEventType.CAMP_EVENT_TRIGGERED,
                camp.getCampaign().getId(), payload, user.getId());
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }
}
