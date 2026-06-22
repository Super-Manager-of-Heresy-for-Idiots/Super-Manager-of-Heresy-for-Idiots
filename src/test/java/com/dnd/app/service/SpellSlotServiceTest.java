package com.dnd.app.service;

import com.dnd.app.domain.CharacterClassLevel;
import com.dnd.app.domain.CharacterSpellSlotUsage;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.response.SpellSlotsResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.UnprocessableEntityException;
import com.dnd.app.repository.CharacterClassLevelRepository;
import com.dnd.app.repository.CharacterSpellSlotUsageRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpellSlotService: вывод максимума ячеек и трекинг расхода")
class SpellSlotServiceTest {

    @Mock private JdbcTemplate jdbc;
    @Mock private PlayerCharacterRepository characterRepository;
    @Mock private UserRepository userRepository;
    @Mock private CharacterClassLevelRepository classLevelRepository;
    @Mock private CharacterSpellSlotUsageRepository usageRepository;
    @Mock private CampaignService campaignService;

    @InjectMocks private SpellSlotService service;

    private final UUID characterId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID classId = UUID.randomUUID();
    private static final String USERNAME = "alice";

    private void ownerAuth() {
        User user = User.builder().id(userId).username(USERNAME).role(Role.PLAYER).build();
        PlayerCharacter character = PlayerCharacter.builder().id(characterId).owner(user).build();
        when(characterRepository.findById(characterId)).thenReturn(Optional.of(character));
        when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
    }

    private void classAtLevel(int level) {
        when(classLevelRepository.findAllByCharacterId(characterId)).thenReturn(List.of(
                CharacterClassLevel.builder().classId(classId).classLevel(level).build()));
    }

    private void slotRows(int classLevel, Map<String, Object>... rows) {
        when(jdbc.queryForList(anyString(), eq(classId), eq(classLevel))).thenReturn(List.of(rows));
    }

    private Map<String, Object> row(String slug, int num) {
        Map<String, Object> m = new HashMap<>();
        m.put("slug", slug);
        m.put("num", num);
        return m;
    }

    private SpellSlotsResponse.SlotLevel level(SpellSlotsResponse r, int lvl) {
        return r.getLevels().stream().filter(s -> s.getSpellLevel() == lvl).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("getSlots выводит max из прогрессии класса, available = max при нулевом расходе")
    void getSlots_derivesMaxFromProgression() {
        ownerAuth();
        classAtLevel(3);
        slotRows(3, row("yacheyki-zaklinaniy-level1", 4), row("yacheyki-zaklinaniy-level2", 2));
        when(usageRepository.findAllByCharacterId(characterId)).thenReturn(List.of());

        SpellSlotsResponse resp = service.getSlots(characterId, USERNAME);

        assertEquals(4, level(resp, 1).getMax());
        assertEquals(4, level(resp, 1).getAvailable());
        assertEquals(2, level(resp, 2).getMax());
        assertEquals(0, level(resp, 1).getExpended());
    }

    @Test
    @DisplayName("Колдун: единственная колонка пакта маппится на уровень ячейки по уровню класса")
    void getSlots_warlockPactColumnMapsToSlotLevel() {
        ownerAuth();
        classAtLevel(5);
        slotRows(5, row("yacheyki-zaklinaniy", 2));
        when(usageRepository.findAllByCharacterId(characterId)).thenReturn(List.of());

        SpellSlotsResponse resp = service.getSlots(characterId, USERNAME);

        assertEquals(2, level(resp, 3).getMax());
        assertEquals(2, level(resp, 3).getAvailable());
    }

    @Test
    @DisplayName("expend увеличивает потраченные на 1 и сохраняет")
    void expend_incrementsExpended() {
        ownerAuth();
        classAtLevel(3);
        slotRows(3, row("yacheyki-zaklinaniy-level1", 2));
        when(usageRepository.findByCharacterIdAndSpellLevel(characterId, 1)).thenReturn(Optional.empty());
        when(usageRepository.findAllByCharacterId(characterId)).thenReturn(List.of());
        when(usageRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));

        service.expend(characterId, USERNAME, 1);

        ArgumentCaptor<CharacterSpellSlotUsage> cap = ArgumentCaptor.forClass(CharacterSpellSlotUsage.class);
        verify(usageRepository).save(cap.capture());
        assertEquals(1, cap.getValue().getExpendedCount());
        assertEquals(1, cap.getValue().getSpellLevel());
    }

    @Test
    @DisplayName("expend на уровне без ячеек -> 422")
    void expend_throwsWhenNoSlots() {
        ownerAuth();
        classAtLevel(3);
        slotRows(3); // нет колонок ячеек
        assertThrows(UnprocessableEntityException.class, () -> service.expend(characterId, USERNAME, 3));
        verify(usageRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("expend когда все ячейки уровня уже потрачены -> 422")
    void expend_throwsWhenAllExpended() {
        ownerAuth();
        classAtLevel(3);
        slotRows(3, row("yacheyki-zaklinaniy-level1", 2));
        when(usageRepository.findByCharacterIdAndSpellLevel(characterId, 1)).thenReturn(Optional.of(
                CharacterSpellSlotUsage.builder().characterId(characterId).spellLevel(1).expendedCount(2).build()));

        assertThrows(UnprocessableEntityException.class, () -> service.expend(characterId, USERNAME, 1));
        verify(usageRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("expend с уровнем вне диапазона 1..9 -> 400")
    void expend_rejectsOutOfRangeLevel() {
        ownerAuth();
        assertThrows(BadRequestException.class, () -> service.expend(characterId, USERNAME, 0));
        assertThrows(BadRequestException.class, () -> service.expend(characterId, USERNAME, 10));
    }

    @Test
    @DisplayName("restoreAll обнуляет потраченное по всем уровням")
    void restoreAll_zeroesExpended() {
        ownerAuth();
        classAtLevel(3);
        slotRows(3, row("yacheyki-zaklinaniy-level1", 2));
        CharacterSpellSlotUsage u1 = CharacterSpellSlotUsage.builder()
                .characterId(characterId).spellLevel(1).expendedCount(2).build();
        CharacterSpellSlotUsage u2 = CharacterSpellSlotUsage.builder()
                .characterId(characterId).spellLevel(2).expendedCount(1).build();
        when(usageRepository.findAllByCharacterId(characterId)).thenReturn(List.of(u1, u2));
        when(usageRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));

        service.restoreAll(characterId, USERNAME);

        assertEquals(0, u1.getExpendedCount());
        assertEquals(0, u2.getExpendedCount());
        verify(usageRepository, times(2)).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("restoreHalf восстанавливает floor(expended/2); неполные не трогает")
    void restoreHalf_restoresFloorHalf() {
        ownerAuth();
        classAtLevel(3);
        slotRows(3, row("yacheyki-zaklinaniy-level1", 6));
        CharacterSpellSlotUsage u1 = CharacterSpellSlotUsage.builder()
                .characterId(characterId).spellLevel(1).expendedCount(5).build();
        CharacterSpellSlotUsage u2 = CharacterSpellSlotUsage.builder()
                .characterId(characterId).spellLevel(2).expendedCount(1).build();
        when(usageRepository.findAllByCharacterId(characterId)).thenReturn(List.of(u1, u2));
        when(usageRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(i -> i.getArgument(0));

        service.restoreHalf(characterId, USERNAME);

        assertEquals(3, u1.getExpendedCount()); // 5 - floor(5/2)=2 -> 3
        assertEquals(1, u2.getExpendedCount()); // floor(1/2)=0 -> без изменений
        verify(usageRepository, times(1)).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Не владелец/не GM/не админ -> AccessDenied")
    void rejectsUnauthorized() {
        User other = User.builder().id(UUID.randomUUID()).username("bob").role(Role.PLAYER).build();
        User owner = User.builder().id(UUID.randomUUID()).username(USERNAME).role(Role.PLAYER).build();
        PlayerCharacter character = PlayerCharacter.builder().id(characterId).owner(owner).build();
        when(characterRepository.findById(characterId)).thenReturn(Optional.of(character));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(other));

        assertThrows(AccessDeniedException.class, () -> service.getSlots(characterId, "bob"));
    }
}
