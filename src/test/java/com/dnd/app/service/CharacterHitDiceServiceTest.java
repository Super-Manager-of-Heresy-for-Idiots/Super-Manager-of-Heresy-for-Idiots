package com.dnd.app.service;

import com.dnd.app.domain.CharacterClassLevel;
import com.dnd.app.domain.CharacterHitDie;
import com.dnd.app.domain.CharacterStat;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.User;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.combat.HpChangeResult;
import com.dnd.app.dto.response.HitDiceSpendResponse;
import com.dnd.app.repository.CharacterClassLevelRepository;
import com.dnd.app.repository.CharacterHitDieRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterHitDiceServiceTest {

    @Mock private CharacterHitDieRepository repository;
    @Mock private CharacterClassLevelRepository classLevelRepository;
    @Mock private PlayerCharacterRepository characterRepository;
    @Mock private UserRepository userRepository;
    @Mock private CampaignService campaignService;
    @Mock private CharacterHpService characterHpService;

    @InjectMocks private CharacterHitDiceService service;

    private final UUID characterId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final String username = "owner";

    @Test
    void provisionCreatesPoolFromClassLevels() {
        PlayerCharacter character = PlayerCharacter.builder().id(characterId).build();
        ContentCharacterClass cls = ContentCharacterClass.builder().hitDie(10).build();
        when(classLevelRepository.findAllByCharacterId(characterId)).thenReturn(List.of(
                CharacterClassLevel.builder().characterId(characterId).characterClass(cls).classLevel(5).build()));
        when(repository.findByCharacterIdAndDie(characterId, 10)).thenReturn(Optional.empty());

        service.provision(character);

        ArgumentCaptor<CharacterHitDie> captor = ArgumentCaptor.forClass(CharacterHitDie.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getDie()).isEqualTo(10);
        assertThat(captor.getValue().getTotal()).isEqualTo(5);
        assertThat(captor.getValue().getRemaining()).isEqualTo(5);
    }

    @Test
    void spendHealsWithConModifierAndDecrements() {
        User owner = User.builder().id(userId).username(username).role(Role.PLAYER).build();
        CharacterStat conStat = CharacterStat.builder()
                .statType(StatType.builder().slug("con").build()).value(14).build(); // +2 modifier
        PlayerCharacter character = PlayerCharacter.builder().id(characterId).owner(owner)
                .stats(new ArrayList<>(List.of(conStat))).build();
        when(characterRepository.findById(characterId)).thenReturn(Optional.of(character));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(owner));
        CharacterHitDie row = CharacterHitDie.builder()
                .id(UUID.randomUUID()).characterId(characterId).die(10).total(5).remaining(4).build();
        when(repository.findByCharacterIdAndDie(characterId, 10)).thenReturn(Optional.of(row));
        when(characterHpService.applyDelta(eq(characterId), eq(16), any(), eq(userId)))
                .thenReturn(new HpChangeResult(characterId, 30, 0, 40, false));

        HitDiceSpendResponse r = service.spend(null, characterId, 10, 2, 12, username);

        // heal = rolledTotal(12) + conMod(+2) * count(2) = 16
        assertThat(r.getHealed()).isEqualTo(16);
        assertThat(row.getRemaining()).isEqualTo(2);
        verify(characterHpService).applyDelta(eq(characterId), eq(16), any(), eq(userId));
    }

    @Test
    void longRestRegainsHalfMinOne() {
        PlayerCharacter character = PlayerCharacter.builder().id(characterId).build();
        when(classLevelRepository.findAllByCharacterId(characterId)).thenReturn(List.of());
        CharacterHitDie row = CharacterHitDie.builder()
                .id(UUID.randomUUID()).characterId(characterId).die(10).total(5).remaining(1).build();
        when(repository.findByCharacterId(characterId)).thenReturn(List.of(row));

        service.restoreOnLongRest(character);

        // spent 4, regain half(5) = 2 → remaining 1 + 2 = 3
        assertThat(row.getRemaining()).isEqualTo(3);
    }
}
