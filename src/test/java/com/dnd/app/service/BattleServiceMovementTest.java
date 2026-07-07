package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.BattleStatus;
import com.dnd.app.domain.enums.CombatantType;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.MovementRequest;
import com.dnd.app.dto.response.MovementContextResponse;
import com.dnd.app.dto.response.MovementResultResponse;
import com.dnd.app.repository.*;
import com.dnd.app.service.combat.ClassAbilityCombatService;
import com.dnd.app.service.combat.DiceRoller;
import com.dnd.app.service.combat.WeaponAttackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("BattleService: серверная валидация и учёт движения (бюджет per-turn)")
class BattleServiceMovementTest {

    @Mock private BattleRepository battleRepository;
    @Mock private BattleCombatantRepository combatantRepository;
    @Mock private PlayerCharacterRepository characterRepository;
    @Mock private UserRepository userRepository;
    @Mock private CampaignService campaignService;
    @Mock private MonsterService monsterService;
    @Mock private CharacterService characterService;
    @Mock private CharacterResourceService characterResourceService;
    @Mock private CharacterEffectService characterEffectService;
    @Mock private WebSocketEventService webSocketEventService;
    @Mock private DiceRoller diceRoller;
    @Mock private WeaponAttackService weaponAttackService;
    @Mock private ClassAbilityCombatService classAbilityCombatService;
    @Mock private ItemInstanceRepository itemInstanceRepository;
    @Mock private SpellRepository spellRepository;
    @Mock private SpellSlotService spellSlotService;
    @Mock private GameplayEventService gameplayEventService;
    @Mock private ModifierAggregator modifierAggregator;
    @Mock private EffectExpirationService effectExpirationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private BattleService battleService;

    private final String username = "gm";
    private final UUID campaignId = UUID.randomUUID();
    private final UUID battleId = UUID.randomUUID();

    private Battle battle;
    private BattleCombatant monsterC; // walk 30, turnOrder 0 (active)
    private BattleCombatant characterC; // speed 30, turnOrder 1

    @BeforeEach
    void setUp() {
        battleService = new BattleService(battleRepository, combatantRepository, characterRepository,
                userRepository, campaignService, monsterService, characterService,
                characterResourceService, characterEffectService, webSocketEventService,
                diceRoller, weaponAttackService, classAbilityCombatService, itemInstanceRepository,
                spellRepository, spellSlotService, objectMapper,
                new CharacterHpService(characterRepository, combatantRepository,
                        webSocketEventService, gameplayEventService),
                modifierAggregator, effectExpirationService);

        User gm = User.builder().id(UUID.randomUUID()).username(username).role(Role.ADMIN).build();
        User playerOwner = User.builder().id(UUID.randomUUID()).username("player").role(Role.PLAYER).build();
        Campaign campaign = Campaign.builder().id(campaignId).build();
        battle = Battle.builder()
                .id(battleId).campaign(campaign).status(BattleStatus.ACTIVE)
                .roundNumber(1).currentTurnIndex(0).build();

        MovementType walk = MovementType.builder().id(UUID.randomUUID()).code("walk").nameRusloc("Ходьба").build();
        MonsterSpeed walkSpeed = MonsterSpeed.builder().id(UUID.randomUUID()).movementType(walk).ft(30).hover(false).build();
        Monster monster = Monster.builder()
                .id(UUID.randomUUID()).nameRusloc("Гоблин")
                .speeds(new ArrayList<>(List.of(walkSpeed))).build();
        monsterC = BattleCombatant.builder()
                .id(UUID.randomUUID()).battle(battle).type(CombatantType.MONSTER).monster(monster)
                .displayName("Гоблин #1").turnOrder(0).currentHp(7).maxHp(7).movementUsedFt(0)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z")).build();

        PlayerCharacter character = PlayerCharacter.builder()
                .id(UUID.randomUUID()).name("Герой").owner(playerOwner).speed(30)
                .currentHp(40).maxHp(40).build();
        characterC = BattleCombatant.builder()
                .id(UUID.randomUUID()).battle(battle).type(CombatantType.CHARACTER).character(character)
                .displayName("Герой").turnOrder(1).currentHp(40).maxHp(40).movementUsedFt(0)
                .createdAt(Instant.parse("2026-01-01T00:00:01Z")).build();

        lenient().when(userRepository.findByUsername(username)).thenReturn(Optional.of(gm));
        lenient().when(campaignService.findCampaign(campaignId)).thenReturn(campaign);
        lenient().when(battleRepository.findByIdAndCampaignIdForUpdate(battleId, campaignId)).thenReturn(Optional.of(battle));
        lenient().when(battleRepository.findByIdAndCampaignId(battleId, campaignId)).thenReturn(Optional.of(battle));
        lenient().when(combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId))
                .thenReturn(List.of(monsterC, characterC));
        lenient().when(combatantRepository.findByIdForUpdate(monsterC.getId())).thenReturn(Optional.of(monsterC));
        lenient().when(combatantRepository.findByIdForUpdate(characterC.getId())).thenReturn(Optional.of(characterC));
        lenient().when(combatantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(battleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private MovementResultResponse move(BattleCombatant c, int feet, boolean gmOverride) {
        return battleService.applyMovement(campaignId, battleId,
                MovementRequest.builder().combatantId(c.getId()).feet(feet).gmOverride(gmOverride).build());
    }

    @Test
    @DisplayName("Активный ход, в пределах скорости: разрешено и бюджет списан")
    void activeWithinBudget_allowedAndSpends() {
        MovementResultResponse r = move(monsterC, 25, false);

        assertTrue(r.isAllowed());
        assertNull(r.getReason());
        assertEquals(30, r.getSpeedFt());
        assertEquals(5, r.getRemainingFt());
        assertTrue(r.isWithinBudget());
        assertEquals(25, monsterC.getMovementUsedFt());
    }

    @Test
    @DisplayName("Накопление за ход: 20 + 15 превышает 30 → второй ход отклонён")
    void accumulatesAndRejectsWhenExceeded() {
        assertTrue(move(monsterC, 20, false).isAllowed());

        MovementResultResponse r = move(monsterC, 15, false);

        assertFalse(r.isAllowed());
        assertEquals("MOVEMENT_BUDGET_EXCEEDED", r.getReason());
        assertEquals(20, monsterC.getMovementUsedFt()); // не изменилось
        assertEquals(10, r.getRemainingFt());
    }

    @Test
    @DisplayName("Одиночный ход сверх скорости отклонён без списания")
    void overBudget_rejected() {
        MovementResultResponse r = move(monsterC, 35, false);

        assertFalse(r.isAllowed());
        assertEquals("MOVEMENT_BUDGET_EXCEEDED", r.getReason());
        assertEquals(0, monsterC.getMovementUsedFt());
    }

    @Test
    @DisplayName("Движение не в свой ход отклонено")
    void notActiveTurn_rejected() {
        MovementResultResponse r = move(characterC, 5, false);

        assertFalse(r.isAllowed());
        assertEquals("NOT_ACTIVE_TURN", r.getReason());
        assertEquals(0, characterC.getMovementUsedFt());
    }

    @Test
    @DisplayName("GM override: проверки пропущены, ход списан и помечен")
    void gmOverride_bypassesChecks() {
        MovementResultResponse r = move(characterC, 100, true); // не её ход и сверх скорости

        assertTrue(r.isAllowed());
        assertTrue(r.isGmOverride());
        assertFalse(r.isWithinBudget());
        assertEquals(100, characterC.getMovementUsedFt());
        assertEquals(-70, r.getRemainingFt());
    }

    @Test
    @DisplayName("Бой не активен: движение отклонено")
    void battleNotActive_rejected() {
        battle.setStatus(BattleStatus.COMPLETED);

        MovementResultResponse r = move(monsterC, 5, false);

        assertFalse(r.isAllowed());
        assertEquals("BATTLE_NOT_ACTIVE", r.getReason());
        assertEquals(0, monsterC.getMovementUsedFt());
    }

    @Test
    @DisplayName("movement-context: активный комбатант и скорости/остатки всех")
    void movementContext_reportsSpeedsAndRemaining() {
        move(monsterC, 10, false);

        MovementContextResponse ctx = battleService.movementContext(campaignId, battleId);

        assertEquals(monsterC.getId(), ctx.getActiveCombatantId());
        assertEquals(1, ctx.getRoundNumber());
        assertEquals(2, ctx.getCombatants().size());
        MovementContextResponse.CombatantMovement m = ctx.getCombatants().stream()
                .filter(e -> e.getCombatantId().equals(monsterC.getId())).findFirst().orElseThrow();
        assertEquals(30, m.getSpeedFt());
        assertEquals(10, m.getMovementUsedFt());
        assertEquals(20, m.getRemainingFt());
    }

    @Test
    @DisplayName("Смена хода сбрасывает бюджет движения комбатанта, начинающего ход")
    void endTurn_resetsMovementBudget() {
        characterC.setMovementUsedFt(20); // персонаж (индекс 1) походил на прошлом круге

        battleService.endTurn(campaignId, battleId, username); // монстр(0) → персонаж(1)

        assertEquals(0, characterC.getMovementUsedFt());
    }
}
