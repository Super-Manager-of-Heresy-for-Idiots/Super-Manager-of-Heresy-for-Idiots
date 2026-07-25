package com.dnd.app.service;

import com.dnd.app.domain.CampParticipant;
import com.dnd.app.domain.CampSession;
import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.CampaignLocation;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.CampInterruptReason;
import com.dnd.app.domain.enums.CampParticipantState;
import com.dnd.app.domain.enums.CampStatus;
import com.dnd.app.domain.enums.CampaignStatus;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.request.CampWatchSlotRequest;
import com.dnd.app.dto.request.CreateCampRequest;
import com.dnd.app.dto.request.InterruptCampRequest;
import com.dnd.app.dto.request.UpdateCampWatchOrderRequest;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.mapper.CampMapper;
import com.dnd.app.repository.CampEventRepository;
import com.dnd.app.repository.CampParticipantRepository;
import com.dnd.app.repository.CampSessionRepository;
import com.dnd.app.repository.CampaignLocationRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CampService: жизненный цикл привала")
class CampServiceTest {

    @Mock private CampSessionRepository campSessionRepository;
    @Mock private CampParticipantRepository campParticipantRepository;
    @Mock private CampEventRepository campEventRepository;
    @Mock private CampaignLocationRepository locationRepository;
    @Mock private PlayerCharacterRepository characterRepository;
    @Mock private UserRepository userRepository;
    @Mock private CampaignService campaignService;
    @Mock private WebSocketEventService webSocketEventService;
    @Mock private CampMapper campMapper;

    private CampService service;

    private final UUID campaignId = UUID.randomUUID();
    private final UUID campId = UUID.randomUUID();
    private final String username = "gm";

    private User gm;
    private Campaign campaign;
    private CampSession camp;

    @BeforeEach
    void setUp() {
        service = new CampService(campSessionRepository, campParticipantRepository, campEventRepository,
                locationRepository, characterRepository, userRepository, campaignService,
                webSocketEventService, campMapper, new ObjectMapper());

        gm = User.builder().id(UUID.randomUUID()).username(username).role(Role.GAME_MASTER).build();
        campaign = Campaign.builder().id(campaignId).status(CampaignStatus.ACTIVE).build();
        camp = CampSession.builder()
                .id(campId)
                .campaign(campaign)
                .name("Привал у Серой ложбины")
                .status(CampStatus.SETTING_UP)
                .createdBy(gm)
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(gm));
        when(campaignService.findCampaign(campaignId)).thenReturn(campaign);
        when(campSessionRepository.findByIdAndCampaignIdForUpdate(campId, campaignId))
                .thenReturn(Optional.of(camp));
        when(campSessionRepository.save(any(CampSession.class))).thenAnswer(i -> i.getArgument(0));
        when(campSessionRepository.saveAndFlush(any(CampSession.class))).thenAnswer(i -> i.getArgument(0));
        when(campParticipantRepository.save(any(CampParticipant.class))).thenAnswer(i -> i.getArgument(0));
    }

    private CampParticipant participantWithSlot(String characterName, Integer slot) {
        return CampParticipant.builder()
                .id(UUID.randomUUID())
                .campSession(camp)
                .character(PlayerCharacter.builder().id(UUID.randomUUID()).name(characterName).build())
                .state(CampParticipantState.NOT_RESTED)
                .watchSlot(slot)
                .build();
    }

    @Test
    @DisplayName("Второй незавершённый привал в кампании не создаётся")
    void createRejectsSecondOpenCamp() {
        when(campSessionRepository.findCurrent(campaignId, CampStatus.COMPLETED)).thenReturn(Optional.of(camp));

        assertThatThrownBy(() -> service.createCamp(campaignId,
                CreateCampRequest.builder().name("Новый привал").build(), username))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("незавершённый привал");
    }

    @Test
    @DisplayName("В привал нельзя взять персонажа чужой кампании")
    void createRejectsCharacterFromAnotherCampaign() {
        UUID characterId = UUID.randomUUID();
        when(campSessionRepository.findCurrent(campaignId, CampStatus.COMPLETED)).thenReturn(Optional.empty());
        when(campParticipantRepository.findByCampSessionIdOrderByCreatedAtAsc(any())).thenReturn(List.of());
        when(characterRepository.findById(characterId)).thenReturn(Optional.of(PlayerCharacter.builder()
                .id(characterId)
                .name("Чужак")
                .campaign(Campaign.builder().id(UUID.randomUUID()).build())
                .build()));

        assertThatThrownBy(() -> service.createCamp(campaignId, CreateCampRequest.builder()
                .name("Привал")
                .participantCharacterIds(List.of(characterId))
                .build(), username))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("не принадлежит этой кампании");
    }

    @Test
    @DisplayName("Локация другой кампании в привал не подставляется")
    void createRejectsForeignLocation() {
        UUID locationId = UUID.randomUUID();
        when(campSessionRepository.findCurrent(campaignId, CampStatus.COMPLETED)).thenReturn(Optional.empty());
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(CampaignLocation.builder()
                .id(locationId)
                .campaign(Campaign.builder().id(UUID.randomUUID()).build())
                .build()));

        assertThatThrownBy(() -> service.createCamp(campaignId, CreateCampRequest.builder()
                .name("Привал")
                .locationId(locationId)
                .build(), username))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("другой кампании");
    }

    @Test
    @DisplayName("Начало привала переводит SETTING_UP в ACTIVE и фиксирует время старта")
    void startMovesSettingUpToActive() {
        service.startCamp(campaignId, campId, username);

        assertThat(camp.getStatus()).isEqualTo(CampStatus.ACTIVE);
        assertThat(camp.getStartedAt()).isNotNull();
        verify(webSocketEventService).sendCampaignEvent(eq(WebSocketEventType.CAMP_UPDATED),
                eq(campaignId), any(), eq(gm.getId()));
    }

    @Test
    @DisplayName("Возврат из прерывания очищает причину и тип отдыха")
    void startFromInterruptedClearsInterruptState() {
        camp.setStatus(CampStatus.INTERRUPTED);
        camp.setInterruptReason(CampInterruptReason.AMBUSH);
        camp.setRestType("long_rest");

        service.startCamp(campaignId, campId, username);

        assertThat(camp.getStatus()).isEqualTo(CampStatus.ACTIVE);
        assertThat(camp.getInterruptReason()).isNull();
        assertThat(camp.getRestType()).isNull();
    }

    @Test
    @DisplayName("Из отдыха нельзя вернуться в ACTIVE напрямую")
    void startRejectedFromResting() {
        camp.setStatus(CampStatus.RESTING);

        assertThatThrownBy(() -> service.startCamp(campaignId, campId, username))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Недопустимый переход");
    }

    @Test
    @DisplayName("Завершённый привал переходов не имеет")
    void completedCampIsTerminal() {
        camp.setStatus(CampStatus.COMPLETED);

        assertThatThrownBy(() -> service.endCamp(campaignId, campId, username))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.startCamp(campaignId, campId, username))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Прерывание фиксирует причину и снимает участников с отдыха")
    void interruptResetsRestingParticipants() {
        camp.setStatus(CampStatus.RESTING);
        CampParticipant resting = CampParticipant.builder()
                .id(UUID.randomUUID())
                .campSession(camp)
                .character(PlayerCharacter.builder().id(UUID.randomUUID()).name("Бренн").build())
                .state(CampParticipantState.RESTING)
                .build();
        when(campParticipantRepository.findByCampSessionIdOrderByCreatedAtAsc(campId))
                .thenReturn(List.of(resting));

        service.interruptCamp(campaignId, campId,
                InterruptCampRequest.builder().reason(CampInterruptReason.AMBUSH).build(), username);

        assertThat(camp.getStatus()).isEqualTo(CampStatus.INTERRUPTED);
        assertThat(camp.getInterruptReason()).isEqualTo(CampInterruptReason.AMBUSH);
        assertThat(camp.getInterruptedAt()).isNotNull();
        assertThat(resting.getState()).isEqualTo(CampParticipantState.NOT_RESTED);
        verify(webSocketEventService).sendCampaignEvent(eq(WebSocketEventType.CAMP_INTERRUPTED),
                eq(campaignId), any(), eq(gm.getId()));
    }

    @Test
    @DisplayName("Прерывание без причины считается ручным решением мастера")
    void interruptDefaultsToManualReason() {
        camp.setStatus(CampStatus.ACTIVE);
        when(campParticipantRepository.findByCampSessionIdOrderByCreatedAtAsc(campId)).thenReturn(List.of());

        service.interruptCamp(campaignId, campId, null, username);

        assertThat(camp.getInterruptReason()).isEqualTo(CampInterruptReason.MANUAL);
    }

    @Test
    @DisplayName("Свёртывание лагеря переводит привал в COMPLETED")
    void endCompletesCamp() {
        camp.setStatus(CampStatus.ACTIVE);

        service.endCamp(campaignId, campId, username);

        assertThat(camp.getStatus()).isEqualTo(CampStatus.COMPLETED);
        assertThat(camp.getEndedAt()).isNotNull();
        verify(webSocketEventService).sendCampaignEvent(eq(WebSocketEventType.CAMP_ENDED),
                eq(campaignId), any(), eq(gm.getId()));
    }

    @Test
    @DisplayName("В дозор нельзя поставить персонажа вне состава привала")
    void watchOrderRejectsOutsiders() {
        UUID outsiderId = UUID.randomUUID();
        when(campParticipantRepository.findByCampSessionIdOrderByCreatedAtAsc(campId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.updateWatchOrder(campaignId, campId,
                UpdateCampWatchOrderRequest.builder()
                        .watchSlotCount(2)
                        .watchSchedule(List.of(CampWatchSlotRequest.builder()
                                .slot(1).label("20:00 — 22:30").characterId(outsiderId).build()))
                        .build(), username))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("вне состава привала");
    }

    @Test
    @DisplayName("Перестановка дозора сохраняет расписание и назначает слот участнику")
    void watchOrderAssignsSlotToParticipant() {
        CampParticipant participant = CampParticipant.builder()
                .id(UUID.randomUUID())
                .campSession(camp)
                .character(PlayerCharacter.builder().id(UUID.randomUUID()).name("Мирелла").build())
                .state(CampParticipantState.NOT_RESTED)
                .build();
        when(campParticipantRepository.findByCampSessionIdOrderByCreatedAtAsc(campId))
                .thenReturn(List.of(participant));

        service.updateWatchOrder(campaignId, campId, UpdateCampWatchOrderRequest.builder()
                .watchSlotCount(4)
                .watchSchedule(List.of(CampWatchSlotRequest.builder()
                        .slot(3).label("01:00 — 03:30")
                        .characterId(participant.getCharacter().getId()).build()))
                .build(), username);

        assertThat(camp.getWatchSlotCount()).isEqualTo(4);
        assertThat(camp.getWatchScheduleJson()).contains("01:00 — 03:30");
        assertThat(participant.getWatchSlot()).isEqualTo(3);
    }

    @Test
    @DisplayName("Гонка разбивки лагеря отдаёт 400, а не ошибку ограничения")
    void concurrentCreateIsRejectedWithBadRequest() {
        when(campSessionRepository.findCurrent(campaignId, CampStatus.COMPLETED)).thenReturn(Optional.empty());
        when(campSessionRepository.saveAndFlush(any(CampSession.class)))
                .thenThrow(new DataIntegrityViolationException("uq_campsessions_active_per_campaign"));

        assertThatThrownBy(() -> service.createCamp(campaignId,
                CreateCampRequest.builder().name("Привал").build(), username))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("незавершённый привал");
    }

    @Test
    @DisplayName("Перестановка дозора освобождает слоты до назначения новых")
    void watchOrderClearsSlotsBeforeReassigning() {
        CampParticipant held = participantWithSlot("Кассиан", 1);
        CampParticipant incoming = participantWithSlot("Мирелла", null);
        when(campParticipantRepository.findByCampSessionIdOrderByCreatedAtAsc(campId))
                .thenReturn(List.of(held, incoming));

        service.updateWatchOrder(campaignId, campId, UpdateCampWatchOrderRequest.builder()
                .watchSlotCount(2)
                .watchSchedule(List.of(CampWatchSlotRequest.builder()
                        .slot(1).characterId(incoming.getCharacter().getId()).build()))
                .build(), username);

        // Освобождение прежнего слота обязано дойти до базы раньше, чем его займёт другой
        // персонаж, иначе частичный уникальный индекс отдаст 500.
        InOrder order = inOrder(campParticipantRepository);
        order.verify(campParticipantRepository).save(held);
        order.verify(campParticipantRepository).flush();
        order.verify(campParticipantRepository).save(incoming);
        assertThat(held.getWatchSlot()).isNull();
        assertThat(incoming.getWatchSlot()).isEqualTo(1);
    }

    @Test
    @DisplayName("Один слот дважды в расписании отклоняется")
    void duplicateWatchSlotIsRejected() {
        when(campParticipantRepository.findByCampSessionIdOrderByCreatedAtAsc(campId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.updateWatchOrder(campaignId, campId,
                UpdateCampWatchOrderRequest.builder()
                        .watchSlotCount(2)
                        .watchSchedule(List.of(
                                CampWatchSlotRequest.builder().slot(1).build(),
                                CampWatchSlotRequest.builder().slot(1).build()))
                        .build(), username))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("дважды");
    }

    @Test
    @DisplayName("Один персонаж в двух слотах дозора отклоняется")
    void duplicateWatchCharacterIsRejected() {
        CampParticipant participant = participantWithSlot("Мирелла", null);
        when(campParticipantRepository.findByCampSessionIdOrderByCreatedAtAsc(campId))
                .thenReturn(List.of(participant));
        UUID characterId = participant.getCharacter().getId();

        assertThatThrownBy(() -> service.updateWatchOrder(campaignId, campId,
                UpdateCampWatchOrderRequest.builder()
                        .watchSlotCount(2)
                        .watchSchedule(List.of(
                                CampWatchSlotRequest.builder().slot(1).characterId(characterId).build(),
                                CampWatchSlotRequest.builder().slot(2).characterId(characterId).build()))
                        .build(), username))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("несколько слотов");
    }

    @Test
    @DisplayName("Уменьшение числа слотов снимает с дозора выпавших участников")
    void shrinkingWatchOrderClearsOutOfRangeSlots() {
        CampParticipant kept = participantWithSlot("Кассиан", 1);
        CampParticipant dropped = participantWithSlot("Тоск", 4);
        when(campParticipantRepository.findByCampSessionIdOrderByCreatedAtAsc(campId))
                .thenReturn(List.of(kept, dropped));

        service.updateWatchOrder(campaignId, campId,
                UpdateCampWatchOrderRequest.builder().watchSlotCount(2).build(), username);

        assertThat(kept.getWatchSlot()).isEqualTo(1);
        assertThat(dropped.getWatchSlot()).isNull();
    }

    @Test
    @DisplayName("Приостановленная кампания запрещает продвижение привала, но не его свёртывание")
    void pausedCampaignAllowsOnlyCleanup() {
        campaign.setStatus(CampaignStatus.PAUSED);
        doThrow(new BadRequestException("Campaign is not active"))
                .when(campaignService).enforceCampaignActive(campaign);
        camp.setStatus(CampStatus.ACTIVE);

        assertThatThrownBy(() -> service.startCamp(campaignId, campId, username))
                .isInstanceOf(BadRequestException.class);

        // Свёртывание должно оставаться доступным, иначе привал зависнет открытым навсегда.
        service.endCamp(campaignId, campId, username);
        assertThat(camp.getStatus()).isEqualTo(CampStatus.COMPLETED);
    }

    @Test
    @DisplayName("Завершённый привал не редактируется")
    void completedCampIsNotEditable() {
        camp.setStatus(CampStatus.COMPLETED);

        assertThatThrownBy(() -> service.updateWatchOrder(campaignId, campId,
                UpdateCampWatchOrderRequest.builder().watchSlotCount(4).build(), username))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Привал завершён");
        verify(campSessionRepository, never()).save(any());
    }
}
