package com.dnd.app.service;

import com.dnd.app.domain.CampParticipant;
import com.dnd.app.domain.CampSession;
import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.CampParticipantState;
import com.dnd.app.domain.enums.CampStatus;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.request.ApplyPartialRestRequest;
import com.dnd.app.dto.request.CompleteCampRestRequest;
import com.dnd.app.dto.request.StartCampRestRequest;
import com.dnd.app.dto.response.CampRestSummaryResponse;
import com.dnd.app.dto.response.RestResult;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.CampParticipantRepository;
import com.dnd.app.repository.CampSessionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CampRestService: групповой отдых привала")
class CampRestServiceTest {

    @Mock private CampSessionRepository campSessionRepository;
    @Mock private CampParticipantRepository campParticipantRepository;
    @Mock private CampParticipantRestExecutor restExecutor;
    @Mock private CampService campService;
    @Mock private WebSocketEventService webSocketEventService;
    @Mock private EntityManager entityManager;

    private CampRestService service;

    private final UUID campaignId = UUID.randomUUID();
    private final UUID campId = UUID.randomUUID();
    private final String username = "gm";

    private User gm;
    private CampSession camp;
    private CampParticipant first;
    private CampParticipant second;

    @BeforeEach
    void setUp() {
        service = new CampRestService(campSessionRepository, campParticipantRepository, restExecutor,
                campService, webSocketEventService, entityManager);

        gm = User.builder().id(UUID.randomUUID()).username(username).role(Role.GAME_MASTER).build();
        camp = CampSession.builder()
                .id(campId)
                .campaign(Campaign.builder().id(campaignId).build())
                .name("Привал у Серой ложбины")
                .status(CampStatus.ACTIVE)
                .createdBy(gm)
                .build();
        first = participant("Кассиан");
        second = participant("Тоск");

        when(campService.requireGmForChange(campaignId, username)).thenReturn(gm);
        when(campService.findCampForUpdate(campaignId, campId)).thenReturn(camp);
        when(campParticipantRepository.findByCampSessionIdOrderByCreatedAtAsc(campId))
                .thenReturn(List.of(first, second));
    }

    private CampParticipant participant(String characterName) {
        return CampParticipant.builder()
                .id(UUID.randomUUID())
                .campSession(camp)
                .character(PlayerCharacter.builder().id(UUID.randomUUID()).name(characterName).build())
                .state(CampParticipantState.NOT_RESTED)
                .build();
    }

    /**
     * Исполнитель отдыха фиксирует состояние участника в собственной транзакции,
     * поэтому в юнит-тесте мок повторяет ровно это поведение.
     */
    private void stubSuccessfulRest(CampParticipant participant, String restType,
                                    CampParticipantState successState) {
        when(restExecutor.rest(campaignId, participant.getId(), participant.getCharacter().getId(),
                restType, successState, username))
                .thenAnswer(invocation -> {
                    participant.setState(successState);
                    participant.setRestResultJson("{\"restType\":\"" + restType + "\"}");
                    return RestResult.builder().restType(restType).build();
                });
    }

    @Test
    @DisplayName("Объявление отдыха переводит привал в RESTING и помечает участников")
    void startRestMovesCampToResting() {
        CampRestSummaryResponse summary = service.startRest(campaignId, campId,
                StartCampRestRequest.builder().restType("long").build(), username);

        assertThat(camp.getStatus()).isEqualTo(CampStatus.RESTING);
        assertThat(camp.getRestType()).isEqualTo("long_rest");
        assertThat(camp.getRestStartedAt()).isNotNull();
        assertThat(first.getState()).isEqualTo(CampParticipantState.RESTING);
        assertThat(second.getState()).isEqualTo(CampParticipantState.RESTING);
        assertThat(summary.getRestType()).isEqualTo("long_rest");
        verify(webSocketEventService).sendCampaignEvent(eq(WebSocketEventType.CAMP_REST_STARTED),
                eq(campaignId), any(), eq(gm.getId()));
    }

    @Test
    @DisplayName("Отдых нельзя объявить, пока лагерь ещё разбивают")
    void startRestRejectedOutsideActive() {
        camp.setStatus(CampStatus.SETTING_UP);

        assertThatThrownBy(() -> service.startRest(campaignId, campId,
                StartCampRestRequest.builder().restType("long").build(), username))
                .isInstanceOf(BadRequestException.class);
        verify(webSocketEventService, never()).sendCampaignEvent(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Неизвестный тип отдыха отклоняется")
    void startRestRejectsUnknownType() {
        assertThatThrownBy(() -> service.startRest(campaignId, campId,
                StartCampRestRequest.builder().restType("siesta").build(), username))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Ошибка одного участника не откатывает отдых остальных")
    void failedParticipantDoesNotRollbackOthers() {
        camp.setStatus(CampStatus.RESTING);
        camp.setRestType("long_rest");
        first.setState(CampParticipantState.RESTING);
        second.setState(CampParticipantState.RESTING);

        stubSuccessfulRest(first, "long_rest", CampParticipantState.RESTED);
        when(restExecutor.rest(campaignId, second.getId(), second.getCharacter().getId(),
                "long_rest", CampParticipantState.RESTED, username))
                .thenThrow(new OptimisticLockingFailureException("лист изменён другим клиентом"));

        CampRestSummaryResponse summary = service.completeRest(campaignId, campId,
                new CompleteCampRestRequest(), username);

        assertThat(first.getState()).isEqualTo(CampParticipantState.RESTED);
        assertThat(first.getRestResultJson()).contains("long_rest");
        assertThat(second.getState()).isEqualTo(CampParticipantState.FAILED);
        assertThat(second.getRestErrorCode()).isEqualTo("REST_TX_CONFLICT");
        assertThat(summary.getRestedCount()).isEqualTo(1);
        assertThat(summary.getFailedCount()).isEqualTo(1);
        assertThat(summary.getFailures()).singleElement()
                .satisfies(failure -> assertThat(failure.getCharacterName()).isEqualTo("Тоск"));
    }

    @Test
    @DisplayName("После успешного отдыха данные участника перечитываются из базы")
    void successfulRestRefreshesParticipantAndCharacter() {
        camp.setStatus(CampStatus.RESTING);
        camp.setRestType("long_rest");
        first.setState(CampParticipantState.RESTING);
        second.setState(CampParticipantState.RESTED);

        stubSuccessfulRest(first, "long_rest", CampParticipantState.RESTED);

        service.completeRest(campaignId, campId,
                CompleteCampRestRequest.builder()
                        .characterIds(List.of(first.getCharacter().getId())).build(), username);

        // Вложенная транзакция уже записала лист персонажа — внешний контекст обязан
        // обновить копии, иначе ответ уйдёт с до-отдыховыми хитами.
        verify(entityManager).refresh(first);
        verify(entityManager).refresh(first.getCharacter());
    }

    @Test
    @DisplayName("Повтор применяется только к переданным персонажам")
    void retryTargetsOnlyRequestedCharacters() {
        camp.setStatus(CampStatus.RESTING);
        camp.setRestType("long_rest");
        first.setState(CampParticipantState.RESTED);
        second.setState(CampParticipantState.FAILED);

        stubSuccessfulRest(second, "long_rest", CampParticipantState.RESTED);

        CampRestSummaryResponse summary = service.completeRest(campaignId, campId,
                CompleteCampRestRequest.builder()
                        .characterIds(List.of(second.getCharacter().getId())).build(), username);

        verify(restExecutor, never()).rest(eq(campaignId), eq(first.getId()), any(), any(), any(), any());
        assertThat(second.getState()).isEqualTo(CampParticipantState.RESTED);
        assertThat(summary.getRestedCount()).isEqualTo(1);
        assertThat(summary.getSkippedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Применение отдыха вне статуса RESTING отклоняется")
    void completeRestRequiresAnnouncedRest() {
        camp.setStatus(CampStatus.ACTIVE);

        assertThatThrownBy(() -> service.completeRest(campaignId, campId,
                new CompleteCampRestRequest(), username))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Частичный отдых доступен только прерванному привалу и применяется как короткий")
    void partialRestAppliesShortRestToInterruptedCamp() {
        camp.setStatus(CampStatus.INTERRUPTED);
        stubSuccessfulRest(first, "short_rest", CampParticipantState.PARTIAL);
        stubSuccessfulRest(second, "short_rest", CampParticipantState.PARTIAL);

        CampRestSummaryResponse summary = service.applyPartialRest(campaignId, campId,
                new ApplyPartialRestRequest(), username);

        assertThat(camp.getApplyPartialRest()).isTrue();
        assertThat(first.getState()).isEqualTo(CampParticipantState.PARTIAL);
        assertThat(second.getState()).isEqualTo(CampParticipantState.PARTIAL);
        assertThat(summary.getPartial()).isTrue();
        assertThat(summary.getRestedCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Частичный отдых нельзя засчитать непрерванному привалу")
    void partialRestRejectedOutsideInterrupted() {
        camp.setStatus(CampStatus.RESTING);

        assertThatThrownBy(() -> service.applyPartialRest(campaignId, campId,
                new ApplyPartialRestRequest(), username))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Отдых без участников не объявляется")
    void startRestRequiresParticipants() {
        when(campParticipantRepository.findByCampSessionIdOrderByCreatedAtAsc(campId))
                .thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> service.startRest(campaignId, campId,
                StartCampRestRequest.builder().restType("long").build(), username))
                .isInstanceOf(BadRequestException.class);
    }
}
