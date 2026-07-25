package com.dnd.app.service;

import com.dnd.app.domain.Battle;
import com.dnd.app.domain.CampEvent;
import com.dnd.app.domain.CampSession;
import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.CampaignLocation;
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
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.mapper.CampMapper;
import com.dnd.app.repository.BattleRepository;
import com.dnd.app.repository.CampEventRepository;
import com.dnd.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CampEventService: журнал привала и засада")
class CampEventServiceTest {

    @Mock private CampEventRepository campEventRepository;
    @Mock private BattleRepository battleRepository;
    @Mock private UserRepository userRepository;
    @Mock private CampService campService;
    @Mock private CampaignService campaignService;
    @Mock private BattleService battleService;
    @Mock private WebSocketEventService webSocketEventService;
    @Mock private CampMapper campMapper;

    private CampEventService service;

    private final UUID campaignId = UUID.randomUUID();
    private final UUID campId = UUID.randomUUID();
    private final UUID battleId = UUID.randomUUID();
    private final UUID monsterId = UUID.randomUUID();
    private final String username = "gm";

    private User gm;
    private CampSession camp;

    @BeforeEach
    void setUp() {
        service = new CampEventService(campEventRepository, battleRepository,
                userRepository, campService, campaignService, battleService, webSocketEventService, campMapper);

        gm = User.builder().id(UUID.randomUUID()).username(username).role(Role.GAME_MASTER).build();
        camp = CampSession.builder()
                .id(campId)
                .campaign(Campaign.builder().id(campaignId).build())
                .name("Привал у Серой ложбины")
                .status(CampStatus.RESTING)
                .location(CampaignLocation.builder().id(UUID.randomUUID()).name("Серая ложбина").build())
                .createdBy(gm)
                .build();

        when(campService.requireGmForChange(campaignId, username)).thenReturn(gm);
        when(campService.findCampForUpdate(campaignId, campId)).thenReturn(camp);
        // Прерывание делегируется CampService — мок повторяет его наблюдаемый эффект,
        // чтобы тест проверял именно факт делегирования, а не дублирующую логику.
        doAnswer(invocation -> {
            camp.setStatus(CampStatus.INTERRUPTED);
            camp.setInterruptReason(invocation.getArgument(1));
            camp.setInterruptEvent(invocation.getArgument(2));
            return null;
        }).when(campService).applyInterruption(any(), any(), any(), any());
        when(campEventRepository.save(any(CampEvent.class))).thenAnswer(invocation -> {
            CampEvent event = invocation.getArgument(0);
            if (event.getId() == null) {
                event.setId(UUID.randomUUID());
            }
            return event;
        });
    }

    private CreateCampEventRequest ambushWithBattle() {
        return CreateCampEventRequest.builder()
                .type(CampEventType.AMBUSH)
                .title("Засада гноллов")
                .createBattle(true)
                .monsters(List.of(AddBattleMonstersRequest.MonsterEntry.builder()
                        .monsterId(monsterId).count(3).build()))
                .build();
    }

    private void stubBattleCreation() {
        when(battleService.createBattle(eq(campaignId), any(CreateBattleRequest.class), eq(username)))
                .thenReturn(BattleResponse.builder().id(battleId).build());
        when(battleRepository.findByIdAndCampaignId(battleId, campaignId))
                .thenReturn(Optional.of(Battle.builder().id(battleId).build()));
    }

    @Test
    @DisplayName("Засада создаёт бой в локации привала и прерывает отдых")
    void ambushCreatesBattleAndInterruptsCamp() {
        stubBattleCreation();

        service.createEvent(campaignId, campId, ambushWithBattle(), username);

        ArgumentCaptor<CreateBattleRequest> battleRequest = ArgumentCaptor.forClass(CreateBattleRequest.class);
        verify(battleService).createBattle(eq(campaignId), battleRequest.capture(), eq(username));
        assertThat(battleRequest.getValue().getName()).isEqualTo("Засада гноллов");
        assertThat(battleRequest.getValue().getLocationId()).isEqualTo(camp.getLocation().getId());
        verify(battleService).addMonsters(eq(campaignId), eq(battleId), any(AddBattleMonstersRequest.class),
                eq(username));

        assertThat(camp.getStatus()).isEqualTo(CampStatus.INTERRUPTED);
        assertThat(camp.getInterruptReason()).isEqualTo(CampInterruptReason.AMBUSH);
        assertThat(camp.getInterruptEvent()).isNotNull();
        // Прерывание идёт единым путём с ручным: состояния участников не расходятся.
        verify(campService).applyInterruption(eq(camp), eq(CampInterruptReason.AMBUSH), any(), eq(gm));
        verify(webSocketEventService).sendCampaignEvent(eq(WebSocketEventType.CAMP_EVENT_TRIGGERED),
                eq(campaignId), any(), eq(gm.getId()));
    }

    @Test
    @DisplayName("Засада без монстров бой не создаёт")
    void ambushWithoutMonstersIsRejected() {
        CreateCampEventRequest request = CreateCampEventRequest.builder()
                .type(CampEventType.AMBUSH)
                .title("Засада")
                .createBattle(true)
                .build();

        assertThatThrownBy(() -> service.createEvent(campaignId, campId, request, username))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("хотя бы один монстр");
        verify(battleService, never()).createBattle(any(), any(), any());
    }

    @Test
    @DisplayName("Заготовка события скрыта от игроков и не трогает статус привала")
    void draftEventStaysHidden() {
        CreateCampEventRequest request = CreateCampEventRequest.builder()
                .type(CampEventType.AMBUSH)
                .title("Гнолли на подходе")
                .triggerNow(false)
                .visibleToPlayers(true)
                .build();

        service.createEvent(campaignId, campId, request, username);

        ArgumentCaptor<CampEvent> saved = ArgumentCaptor.forClass(CampEvent.class);
        verify(campEventRepository).save(saved.capture());
        assertThat(saved.getValue().getVisibleToPlayers()).isFalse();
        assertThat(saved.getValue().getTriggeredAt()).isNull();
        assertThat(camp.getStatus()).isEqualTo(CampStatus.RESTING);
        verify(webSocketEventService, never()).sendCampaignEvent(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Триггер заготовки открывает её игрокам и прерывает привал засадой")
    void triggerRevealsDraftAndInterrupts() {
        CampEvent draft = CampEvent.builder()
                .id(UUID.randomUUID())
                .campSession(camp)
                .type(CampEventType.AMBUSH)
                .title("Гнолли на подходе")
                .visibleToPlayers(false)
                .createdBy(gm)
                .build();
        when(campEventRepository.findByIdAndCampSessionId(draft.getId(), campId)).thenReturn(Optional.of(draft));

        service.triggerEvent(campaignId, campId, draft.getId(), username);

        assertThat(draft.getTriggeredAt()).isNotNull();
        assertThat(draft.getVisibleToPlayers()).isTrue();
        assertThat(camp.getStatus()).isEqualTo(CampStatus.INTERRUPTED);
        assertThat(camp.getInterruptEvent()).isEqualTo(draft);
    }

    @Test
    @DisplayName("Сработавшее событие повторно не триггерится")
    void triggeredEventCannotFireTwice() {
        CampEvent event = CampEvent.builder()
                .id(UUID.randomUUID())
                .campSession(camp)
                .type(CampEventType.STORY)
                .title("Кострище")
                .triggeredAt(Instant.now())
                .createdBy(gm)
                .build();
        when(campEventRepository.findByIdAndCampSessionId(event.getId(), campId)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.triggerEvent(campaignId, campId, event.getId(), username))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("уже сработало");
    }

    @Test
    @DisplayName("Засада во время разбивки лагеря статус не ломает")
    void ambushDuringSetupDoesNotInterrupt() {
        camp.setStatus(CampStatus.SETTING_UP);
        stubBattleCreation();

        service.createEvent(campaignId, campId, ambushWithBattle(), username);

        assertThat(camp.getStatus()).isEqualTo(CampStatus.SETTING_UP);
        assertThat(camp.getInterruptReason()).isNull();
        verify(campService, never()).applyInterruption(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Событие, прервавшее привал, удалить нельзя")
    void interruptingEventCannotBeDeleted() {
        CampEvent event = CampEvent.builder()
                .id(UUID.randomUUID())
                .campSession(camp)
                .type(CampEventType.AMBUSH)
                .title("Засада гноллов")
                .createdBy(gm)
                .build();
        camp.setInterruptEvent(event);
        when(campEventRepository.findByIdAndCampSessionId(event.getId(), campId)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.deleteEvent(campaignId, campId, event.getId(), username))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("прервавшее привал");
        verify(campEventRepository, never()).delete(any());
    }
}
