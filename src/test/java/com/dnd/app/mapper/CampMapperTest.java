package com.dnd.app.mapper;

import com.dnd.app.domain.CampParticipant;
import com.dnd.app.domain.CampSession;
import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.enums.CampParticipantState;
import com.dnd.app.domain.enums.CampStatus;
import com.dnd.app.dto.response.CampParticipantResponse;
import com.dnd.app.repository.CharacterClassLevelRepository;
import com.dnd.app.repository.CharacterHitDieRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CampMapper: сборка ответа привала")
class CampMapperTest {

    @Mock private CharacterHitDieRepository hitDieRepository;
    @Mock private CharacterClassLevelRepository classLevelRepository;

    private CampMapper mapper;

    private CampSession camp;
    private CampParticipant participant;

    @BeforeEach
    void setUp() {
        mapper = new CampMapper(hitDieRepository, classLevelRepository, new ObjectMapper());

        camp = CampSession.builder()
                .id(UUID.randomUUID())
                .campaign(Campaign.builder().id(UUID.randomUUID()).build())
                .name("Привал")
                .status(CampStatus.ACTIVE)
                .watchSlotCount(2)
                .build();
        participant = CampParticipant.builder()
                .id(UUID.randomUUID())
                .campSession(camp)
                .character(PlayerCharacter.builder().id(UUID.randomUUID()).name("Кассиан").build())
                .state(CampParticipantState.NOT_RESTED)
                .watchSlot(1)
                .build();

        when(hitDieRepository.findByCharacterId(any())).thenReturn(List.of());
        when(classLevelRepository.findAllByCharacterId(any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("Слот дозора без метки времени не роняет сборку карточки участника")
    void watchSlotWithoutLabelDoesNotThrow() {
        // Регрессия: findFirst() на null-метке бросал NPE при построении ответа.
        camp.setWatchScheduleJson("[{\"slot\":1,\"label\":null},{\"slot\":2,\"label\":\"01:00\"}]");

        CampParticipantResponse response = mapper.toParticipantResponse(participant);

        assertThat(response.getWatchSlot()).isEqualTo(1);
        assertThat(response.getWatchSlotLabel()).isNull();
    }

    @Test
    @DisplayName("Метка времени слота попадает в карточку участника")
    void watchSlotLabelIsResolvedFromSchedule() {
        camp.setWatchScheduleJson("[{\"slot\":1,\"label\":\"20:00 — 22:30\"}]");

        CampParticipantResponse response = mapper.toParticipantResponse(participant);

        assertThat(response.getWatchSlotLabel()).isEqualTo("20:00 — 22:30");
    }

    @Test
    @DisplayName("Повреждённое расписание дозора не роняет ответ")
    void corruptedScheduleFallsBackToEmpty() {
        camp.setWatchScheduleJson("не json");

        CampParticipantResponse response = mapper.toParticipantResponse(participant);

        assertThat(response.getWatchSlot()).isEqualTo(1);
        assertThat(response.getWatchSlotLabel()).isNull();
    }
}
