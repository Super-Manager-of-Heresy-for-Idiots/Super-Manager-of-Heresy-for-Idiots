package com.dnd.app.service;

import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.combat.HpChangeResult;
import com.dnd.app.dto.response.RestResult;
import com.dnd.app.dto.response.SpellSlotsResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class RestOrchestrationServiceTest {

    @Mock private PlayerCharacterRepository characterRepository;
    @Mock private UserRepository userRepository;
    @Mock private CampaignService campaignService;
    @Mock private CharacterResourceService characterResourceService;
    @Mock private RestFeatureRuntimeService restFeatureRuntimeService;
    @Mock private SpellSlotService spellSlotService;
    @Mock private CharacterHpService characterHpService;

    @InjectMocks private RestOrchestrationService service;

    private final UUID campaignId = UUID.randomUUID();
    private final UUID characterId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final String username = "player";

    private PlayerCharacter ownedCharacter() {
        User owner = User.builder().id(userId).username(username).role(Role.PLAYER).build();
        return PlayerCharacter.builder()
                .id(characterId).owner(owner)
                .campaign(Campaign.builder().id(campaignId).build())
                .currentHp(5).maxHp(20).build();
    }

    private User owner() {
        return User.builder().id(userId).username(username).role(Role.PLAYER).build();
    }

    @Test
    void longRestRestoresEverySubsystemInOneCall() {
        PlayerCharacter character = ownedCharacter();
        when(characterRepository.findById(characterId)).thenReturn(Optional.of(character));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(owner()));
        when(characterResourceService.restReset(characterId, "long_rest", username)).thenReturn(List.of());
        when(restFeatureRuntimeService.complete(character, "long_rest")).thenReturn(List.of());
        when(spellSlotService.restoreAll(characterId, username))
                .thenReturn(SpellSlotsResponse.builder().levels(List.of()).build());
        when(characterHpService.restoreToFull(characterId, campaignId, userId))
                .thenReturn(new HpChangeResult(characterId, 20, 0, 20, false));

        // "long" shorthand is normalized to the canonical code.
        RestResult result = service.rest(campaignId, characterId, "long", username);

        assertThat(result.getRestType()).isEqualTo("long_rest");
        verify(characterResourceService).restReset(characterId, "long_rest", username);
        verify(restFeatureRuntimeService).complete(character, "long_rest");
        verify(spellSlotService).restoreAll(characterId, username);
        verify(characterHpService).restoreToFull(characterId, campaignId, userId);
    }

    @Test
    void shortRestSkipsSpellSlotsAndHp() {
        PlayerCharacter character = ownedCharacter();
        when(characterRepository.findById(characterId)).thenReturn(Optional.of(character));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(owner()));
        when(characterResourceService.restReset(characterId, "short_rest", username)).thenReturn(List.of());
        when(restFeatureRuntimeService.complete(character, "short_rest")).thenReturn(List.of());

        RestResult result = service.rest(campaignId, characterId, "short", username);

        assertThat(result.getRestType()).isEqualTo("short_rest");
        assertThat(result.getSpellSlots()).isNull();
        assertThat(result.getHp()).isNull();
        verify(spellSlotService, never()).restoreAll(any(), any());
        verify(characterHpService, never()).restoreToFull(any(), any(), any());
    }

    @Test
    void unauthorizedUserCannotRest() {
        PlayerCharacter character = ownedCharacter();
        User stranger = User.builder().id(UUID.randomUUID()).username("stranger").role(Role.PLAYER).build();
        when(characterRepository.findById(characterId)).thenReturn(Optional.of(character));
        when(userRepository.findByUsername("stranger")).thenReturn(Optional.of(stranger));
        when(campaignService.isGmInCampaign(eq(campaignId), any())).thenReturn(false);

        assertThatThrownBy(() -> service.rest(campaignId, characterId, "long", "stranger"))
                .isInstanceOf(AccessDeniedException.class);
        verify(characterResourceService, never()).restReset(any(), any(), any());
        verify(characterHpService, never()).restoreToFull(any(), any(), any());
    }
}
