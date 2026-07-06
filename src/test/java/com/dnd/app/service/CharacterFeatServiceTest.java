package com.dnd.app.service;

import com.dnd.app.domain.CharacterFeat;
import com.dnd.app.domain.Feat;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.response.CharacterFeatResponse;
import com.dnd.app.repository.CharacterFeatRepository;
import com.dnd.app.repository.FeatRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterFeatServiceTest {

    @Mock private CharacterFeatRepository characterFeatRepository;
    @Mock private PlayerCharacterRepository characterRepository;
    @Mock private FeatRepository featRepository;
    @Mock private UserRepository userRepository;
    @Mock private CampaignService campaignService;
    @Mock private CharacterResourceService characterResourceService;

    @InjectMocks private CharacterFeatService service;

    private final UUID characterId = UUID.randomUUID();
    private final UUID featId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final String username = "owner";

    private PlayerCharacter ownedCharacter() {
        User owner = User.builder().id(userId).username(username).role(Role.PLAYER).build();
        return PlayerCharacter.builder().id(characterId).owner(owner).build();
    }

    private User owner() {
        return User.builder().id(userId).username(username).role(Role.PLAYER).build();
    }

    @Test
    void addRecordsFeatAndAutoProvisionsBoundResources() {
        PlayerCharacter character = ownedCharacter();
        when(characterRepository.findById(characterId)).thenReturn(Optional.of(character));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(owner()));
        when(featRepository.findById(featId)).thenReturn(Optional.of(Feat.builder().id(featId).nameRu("Удачливый").build()));
        when(characterFeatRepository.findByCharacterIdAndFeatId(characterId, featId)).thenReturn(Optional.empty());
        when(characterFeatRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CharacterFeatResponse r = service.add(characterId, featId, username);

        assertThat(r.getFeatId()).isEqualTo(featId);
        assertThat(r.getFeatName()).isEqualTo("Удачливый");
        verify(characterFeatRepository).save(any(CharacterFeat.class));
        verify(characterResourceService).provisionFeatResources(eq(character), eq(List.of(featId)));
    }

    @Test
    void addIsIdempotentWhenFeatAlreadyPresent() {
        PlayerCharacter character = ownedCharacter();
        when(characterRepository.findById(characterId)).thenReturn(Optional.of(character));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(owner()));
        when(featRepository.findById(featId)).thenReturn(Optional.of(Feat.builder().id(featId).nameRu("X").build()));
        when(characterFeatRepository.findByCharacterIdAndFeatId(characterId, featId))
                .thenReturn(Optional.of(CharacterFeat.builder()
                        .id(UUID.randomUUID()).characterId(characterId).featId(featId).source("manual").build()));

        service.add(characterId, featId, username);

        verify(characterFeatRepository, never()).save(any());
        verify(characterResourceService, never()).provisionFeatResources(any(), any());
    }

    @Test
    void grantFromSourceSkipsWhenFeatAlreadyPresent() {
        when(characterFeatRepository.existsByCharacterIdAndFeatId(characterId, featId)).thenReturn(true);

        service.grantFromSource(ownedCharacter(), featId, "background");

        verify(characterFeatRepository, never()).save(any());
        verify(characterResourceService, never()).provisionFeatResources(any(), any());
    }
}
