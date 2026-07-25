package com.dnd.app.service;

import com.dnd.app.domain.CampActivity;
import com.dnd.app.domain.CampParticipant;
import com.dnd.app.domain.CampSession;
import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.CampStatus;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.request.AssignCampActivityRequest;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.mapper.CampMapper;
import com.dnd.app.repository.CampActivityRepository;
import com.dnd.app.repository.CampParticipantRepository;
import com.dnd.app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
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
@DisplayName("CampActivityService: даунтайм-активности привала")
class CampActivityServiceTest {

    @Mock private CampActivityRepository campActivityRepository;
    @Mock private CampParticipantRepository campParticipantRepository;
    @Mock private UserRepository userRepository;
    @Mock private CampService campService;
    @Mock private CampaignService campaignService;
    @Mock private WebSocketEventService webSocketEventService;
    @Mock private CampMapper campMapper;

    private CampActivityService service;

    private final UUID campaignId = UUID.randomUUID();
    private final UUID campId = UUID.randomUUID();
    private final UUID characterId = UUID.randomUUID();
    private final String username = "gm";

    private User gm;
    private CampSession camp;
    private CampParticipant participant;

    @BeforeEach
    void setUp() {
        service = new CampActivityService(campActivityRepository, campParticipantRepository, userRepository,
                campService, campaignService, webSocketEventService, campMapper);

        gm = User.builder().id(UUID.randomUUID()).username(username).role(Role.GAME_MASTER).build();
        camp = CampSession.builder()
                .id(campId)
                .campaign(Campaign.builder().id(campaignId).build())
                .status(CampStatus.ACTIVE)
                .createdBy(gm)
                .build();
        participant = CampParticipant.builder()
                .id(UUID.randomUUID())
                .campSession(camp)
                .character(PlayerCharacter.builder().id(characterId).name("Бренн").build())
                .build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(gm));
        when(campaignService.findCampaign(campaignId)).thenReturn(camp.getCampaign());
        when(campService.requireGmForChange(campaignId, username)).thenReturn(gm);
        when(campService.findCampForUpdate(campaignId, campId)).thenReturn(camp);
        when(campParticipantRepository.findByCampSessionIdAndCharacterId(campId, characterId))
                .thenReturn(Optional.of(participant));
        when(campParticipantRepository.save(any(CampParticipant.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Назначение активности сохраняет заметку и рассылает обновление участника")
    void assignActivityStoresNote() {
        UUID activityId = UUID.randomUUID();
        CampActivity activity = CampActivity.builder().id(activityId).name("Готовка").build();
        when(campActivityRepository.findAvailableForCampaign(activityId, campaignId))
                .thenReturn(Optional.of(activity));

        service.assignActivity(campaignId, campId, characterId, AssignCampActivityRequest.builder()
                .activityId(activityId).note("Тушёнка из вчерашнего кабана").build(), username);

        assertThat(participant.getActivity()).isEqualTo(activity);
        assertThat(participant.getActivityNote()).isEqualTo("Тушёнка из вчерашнего кабана");
        verify(webSocketEventService).sendCampaignEvent(eq(WebSocketEventType.CAMP_PARTICIPANT_UPDATED),
                eq(campaignId), eq(characterId), any(), eq(gm.getId()));
    }

    @Test
    @DisplayName("Пустой activityId снимает активность с участника")
    void emptyActivityClearsAssignment() {
        participant.setActivity(CampActivity.builder().id(UUID.randomUUID()).name("Ковка").build());
        participant.setActivityNote("правит щит");

        service.assignActivity(campaignId, campId, characterId, new AssignCampActivityRequest(), username);

        assertThat(participant.getActivity()).isNull();
        assertThat(participant.getActivityNote()).isNull();
    }

    @Test
    @DisplayName("Активность чужой кампании не назначается")
    void foreignActivityIsRejected() {
        UUID activityId = UUID.randomUUID();
        when(campActivityRepository.findAvailableForCampaign(activityId, campaignId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignActivity(campaignId, campId, characterId,
                AssignCampActivityRequest.builder().activityId(activityId).build(), username))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Персонажу вне состава привала активность не назначить")
    void outsiderCannotGetActivity() {
        UUID outsiderId = UUID.randomUUID();
        when(campParticipantRepository.findByCampSessionIdAndCharacterId(campId, outsiderId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignActivity(campaignId, campId, outsiderId,
                new AssignCampActivityRequest(), username))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("не участвует в привале");
    }

    @Test
    @DisplayName("Системная активность не удаляется как кастомная")
    void systemActivityCannotBeDeleted() {
        UUID activityId = UUID.randomUUID();
        when(campActivityRepository.findByIdAndCampaignId(activityId, campaignId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteActivity(campaignId, activityId, username))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(campActivityRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Удаление кастомной активности снимает её у участников")
    void deleteActivityClearsAssignments() {
        UUID activityId = UUID.randomUUID();
        CampActivity activity = CampActivity.builder().id(activityId).name("Допрос пленника").build();
        participant.setActivity(activity);
        when(campActivityRepository.findByIdAndCampaignId(activityId, campaignId)).thenReturn(Optional.of(activity));
        when(campParticipantRepository.findByActivityId(activityId)).thenReturn(List.of(participant));

        service.deleteActivity(campaignId, activityId, username);

        assertThat(participant.getActivity()).isNull();
        verify(campActivityRepository).delete(activity);
    }
}
