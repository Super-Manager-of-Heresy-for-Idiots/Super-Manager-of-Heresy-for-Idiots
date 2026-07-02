package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.AttackRollMode;
import com.dnd.app.domain.enums.BattleStatus;
import com.dnd.app.domain.enums.CombatantType;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.request.AddBattleMonstersRequest;
import com.dnd.app.dto.request.AdjustActionEconomyRequest;
import com.dnd.app.dto.request.ApplyCombatantHpRequest;
import com.dnd.app.dto.request.BattleAttackRequest;
import com.dnd.app.dto.request.BattleUseItemRequest;
import com.dnd.app.dto.request.CreateBattleRequest;
import com.dnd.app.dto.request.JoinBattleRequest;
import com.dnd.app.dto.request.SpendActionRequest;
import com.dnd.app.dto.request.UpdateBattleXpRequest;
import com.dnd.app.dto.response.*;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import com.dnd.app.service.combat.AttackResolver;
import com.dnd.app.service.combat.ClassAbilityCombatService;
import com.dnd.app.service.combat.CombatCalculator;
import com.dnd.app.service.combat.DiceRoller;
import com.dnd.app.service.combat.WeaponAttackService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Lifecycle and turn logic of a campaign battle. The GM assembles a monster group (preview of
 * average danger / total xp), starts the fight (rolling initiative for every monster), players
 * join their characters with a d20, and the shared initiative tracker drives turn passing.
 * Every state change is broadcast to the campaign topic so all participants stay in sync; the
 * client re-fetches the authoritative {@link BattleResponse} on each ping.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BattleService {

    /** Well-known code of the system Dexterity stat (used for initiative). */
    private static final String DEX_CODE = "dex";

    private final BattleRepository battleRepository;
    private final BattleCombatantRepository combatantRepository;
    private final PlayerCharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final MonsterService monsterService;
    private final CharacterService characterService;
    private final CharacterResourceService characterResourceService;
    private final CharacterEffectService characterEffectService;
    private final WebSocketEventService webSocketEventService;
    private final DiceRoller diceRoller;
    private final WeaponAttackService weaponAttackService;
    private final ClassAbilityCombatService classAbilityCombatService;
    private final ItemInstanceRepository itemInstanceRepository;
    private final SpellRepository spellRepository;
    private final SpellSlotService spellSlotService;
    private final ObjectMapper objectMapper;

    // ================================ Lifecycle ================================

    @Transactional
    public BattleResponse createBattle(UUID campaignId, CreateBattleRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        campaignService.enforceCampaignActive(campaign);

        Battle battle = Battle.builder()
                .campaign(campaign)
                .name(request != null ? request.getName() : null)
                .status(BattleStatus.ASSEMBLING)
                .createdBy(user)
                .build();
        battle = battleRepository.save(battle);

        log.info("Battle created: id={}, campaignId={}, by={}", battle.getId(), campaignId, username);
        return toResponse(battle, List.of());
    }

    @Transactional(readOnly = true)
    public List<BattleResponse> listBattles(UUID campaignId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);

        return battleRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId).stream()
                .map(b -> toResponse(b, combatantRepository.findByBattleIdOrderByTurnOrderAsc(b.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public BattleResponse getBattle(UUID campaignId, UUID battleId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        Battle battle = findBattle(battleId, campaignId);
        return toResponse(battle, orderedCombatants(battleId));
    }

    @Transactional
    public void endBattle(UUID campaignId, UUID battleId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Battle battle = findBattle(battleId, campaignId);

        if (battle.getStatus() == BattleStatus.COMPLETED) {
            throw new BadRequestException("Battle is already completed");
        }
        battle.setStatus(BattleStatus.COMPLETED);
        battle.setEndedAt(Instant.now());
        battleRepository.save(battle);

        log.info("Battle ended: id={}, by={}", battleId, username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_ENDED, campaignId,
                java.util.Map.of("battleId", battleId), user.getId());
    }

    // ============================== Group assembly ==============================

    @Transactional
    public BattleResponse addMonsters(UUID campaignId, UUID battleId,
                                      AddBattleMonstersRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Battle battle = findBattle(battleId, campaignId);
        requireStatus(battle, BattleStatus.ASSEMBLING, "Monsters can only be added while assembling the group");

        for (AddBattleMonstersRequest.MonsterEntry entry : request.getMonsters()) {
            Monster monster = monsterService.getUsableCampaignMonster(campaignId, entry.getMonsterId(), username);
            int existing = (int) combatantRepository
                    .countByBattleIdAndTypeAndMonsterId(battleId, CombatantType.MONSTER, monster.getId());
            for (int i = 1; i <= entry.getCount(); i++) {
                int index = existing + i;
                BattleCombatant combatant = BattleCombatant.builder()
                        .battle(battle)
                        .type(CombatantType.MONSTER)
                        .monster(monster)
                        .displayName(monster.getNameRusloc() + " #" + index)
                        .instanceIndex(index)
                        .dexTiebreak(monster.getDexScore() != null ? monster.getDexScore().intValue() : null)
                        .currentHp(monster.getHpAverage())
                        .maxHp(monster.getHpAverage())
                        .addedBy(user)
                        .build();
                combatantRepository.save(combatant);
            }
        }

        log.info("Monsters added to battle: battleId={}, entries={}, by={}",
                battleId, request.getMonsters().size(), username);
        List<BattleCombatant> combatants = orderedCombatants(battleId);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_UPDATED, campaignId,
                java.util.Map.of("battleId", battleId), user.getId());
        return toResponse(battle, combatants);
    }

    @Transactional
    public BattleResponse removeCombatant(UUID campaignId, UUID battleId, UUID combatantId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Battle battle = findBattle(battleId, campaignId);
        requireStatus(battle, BattleStatus.ASSEMBLING, "Combatants can only be removed while assembling the group");

        BattleCombatant combatant = combatantRepository.findById(combatantId)
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found"));
        if (!combatant.getBattle().getId().equals(battleId)) {
            throw new ResourceNotFoundException("Combatant does not belong to this battle");
        }
        combatantRepository.delete(combatant);

        log.info("Combatant removed from battle: battleId={}, combatantId={}, by={}", battleId, combatantId, username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_UPDATED, campaignId,
                java.util.Map.of("battleId", battleId), user.getId());
        return toResponse(battle, orderedCombatants(battleId));
    }

    @Transactional
    public BattleResponse setOverrideXp(UUID campaignId, UUID battleId,
                                        UpdateBattleXpRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Battle battle = findBattle(battleId, campaignId);
        if (battle.getStatus() == BattleStatus.COMPLETED) {
            throw new BadRequestException("Cannot change XP of a completed battle");
        }

        battle.setOverrideXp(request.getOverrideXp());
        battleRepository.save(battle);

        log.info("Battle XP override set: battleId={}, overrideXp={}, by={}",
                battleId, request.getOverrideXp(), username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_UPDATED, campaignId,
                java.util.Map.of("battleId", battleId), user.getId());
        return toResponse(battle, orderedCombatants(battleId));
    }

    // ================================== Start ==================================

    @Transactional
    public BattleResponse startBattle(UUID campaignId, UUID battleId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        // Lock the battle row so a second concurrent start can't also roll initiative / reorder.
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ASSEMBLING, "Battle has already started");

        List<BattleCombatant> combatants = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        if (combatants.isEmpty()) {
            throw new BadRequestException("Cannot start a battle with no combatants");
        }

        for (BattleCombatant c : combatants) {
            if (c.getType() == CombatantType.MONSTER) {
                int roll = diceRoller.rollD20();
                int dex = c.getMonster().getDexScore() != null ? c.getMonster().getDexScore().intValue() : 10;
                Integer bonus = c.getMonster().getInitiativeBonus() != null
                        ? c.getMonster().getInitiativeBonus().intValue() : null;
                c.setInitiativeRoll(roll);
                c.setInitiative(CombatCalculator.monsterInitiative(roll, bonus, dex));
            }
        }
        CombatCalculator.orderTracker(combatants);
        combatantRepository.saveAll(combatants);

        battle.setStatus(BattleStatus.ACTIVE);
        battle.setRoundNumber(1);
        battle.setCurrentTurnIndex(0);
        battle.setStartedAt(Instant.now());
        battleRepository.save(battle);

        log.info("Battle started: id={}, combatants={}, by={}", battleId, combatants.size(), username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_STARTED, campaignId,
                java.util.Map.of("battleId", battleId), user.getId());
        return toResponse(battle, combatants);
    }

    // =============================== Player join ===============================

    @Transactional
    public BattleResponse joinCharacters(UUID campaignId, UUID battleId,
                                         JoinBattleRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        // Lock the battle row so concurrent joins serialize their tracker reorder + index re-anchor.
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Characters can only join an active battle");

        boolean gm = user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId());

        // Remember whose turn it is so the order stays anchored after we re-sort
        List<BattleCombatant> before = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        UUID activeId = activeCombatantId(battle, before);

        for (JoinBattleRequest.CharacterJoin join : request.getCharacters()) {
            PlayerCharacter character = characterRepository.findById(join.getCharacterId())
                    .orElseThrow(() -> new ResourceNotFoundException("Character not found"));

            if (character.getCampaign() == null || !character.getCampaign().getId().equals(campaignId)) {
                throw new BadRequestException("Character does not belong to this campaign");
            }
            if (!gm && !character.getOwner().getId().equals(user.getId())) {
                throw new AccessDeniedException("You can only add your own characters to the battle");
            }
            if (combatantRepository.existsByBattleIdAndCharacterId(battleId, character.getId())) {
                throw new BadRequestException("Character '" + character.getName() + "' is already in this battle");
            }

            int d20 = (join.getD20() != null) ? join.getD20() : diceRoller.rollD20();
            int dexValue = dexValue(character);
            int dexBuff = dexBuffBonus(character);
            int initiative = CombatCalculator.characterInitiative(d20, dexValue, dexBuff);

            BattleCombatant combatant = BattleCombatant.builder()
                    .battle(battle)
                    .type(CombatantType.CHARACTER)
                    .character(character)
                    .displayName(character.getName())
                    .initiativeRoll(d20)
                    .initiative(initiative)
                    .dexTiebreak(dexValue)
                    .currentHp(character.getCurrentHp())
                    .maxHp(character.getMaxHp())
                    .addedBy(user)
                    .build();
            combatantRepository.save(combatant);
        }

        // Re-order the whole tracker and keep the turn on the same combatant
        List<BattleCombatant> after = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        CombatCalculator.orderTracker(after);
        combatantRepository.saveAll(after);
        battle.setCurrentTurnIndex(
                CombatCalculator.resolveCurrentIndex(after, activeId, battle.getCurrentTurnIndex()));
        battleRepository.save(battle);

        log.info("Characters joined battle: battleId={}, count={}, by={}",
                battleId, request.getCharacters().size(), username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.COMBATANT_JOINED, campaignId,
                java.util.Map.of("battleId", battleId), user.getId());
        return toResponse(battle, after);
    }

    /**
     * The fixed part of a character's initiative — Dexterity modifier plus the net of active
     * Dexterity buffs/debuffs — so the client can preview the final initiative live as the
     * player enters or rolls a d20 ({@code initiative = d20 + bonus}). Same math the join uses.
     */
    @Transactional(readOnly = true)
    public int getCharacterInitiativeBonus(UUID campaignId, UUID battleId, UUID characterId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        findBattle(battleId, campaignId);

        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        if (character.getCampaign() == null || !character.getCampaign().getId().equals(campaignId)) {
            throw new BadRequestException("Character does not belong to this campaign");
        }
        boolean gm = user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId());
        if (!gm && !character.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can only preview your own characters");
        }
        return CombatCalculator.abilityModifier(dexValue(character)) + dexBuffBonus(character);
    }

    // ============================== Turn passing ===============================

    @Transactional
    public BattleResponse endTurn(UUID campaignId, UUID battleId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        // Row-lock the battle so two simultaneous end-turn calls can't both advance it.
        Battle battle = battleRepository.findByIdAndCampaignIdForUpdate(battleId, campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Battle not found"));
        requireStatus(battle, BattleStatus.ACTIVE, "Turns can only be passed in an active battle");

        List<BattleCombatant> combatants = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        if (combatants.isEmpty()) {
            throw new BadRequestException("Battle has no combatants");
        }

        int currentIndex = clampIndex(battle.getCurrentTurnIndex(), combatants.size());
        BattleCombatant current = combatants.get(currentIndex);
        enforceCanEndTurn(campaignId, user, current);

        int nextIndex = currentIndex + 1;
        if (nextIndex >= combatants.size()) {
            nextIndex = 0;
            battle.setRoundNumber(battle.getRoundNumber() + 1);
            // New round: tick down timed effects on every joined character
            for (BattleCombatant c : combatants) {
                if (c.getType() == CombatantType.CHARACTER && c.getCharacter() != null) {
                    characterEffectService.decrementRounds(c.getCharacter().getId());
                }
            }
        }
        battle.setCurrentTurnIndex(nextIndex);
        battleRepository.save(battle);

        // The combatant now on turn starts with a fresh action economy.
        BattleCombatant nowOnTurn = combatants.get(nextIndex);
        resetActionEconomy(nowOnTurn);

        log.info("Turn passed: battleId={}, newIndex={}, round={}, by={}",
                battleId, nextIndex, battle.getRoundNumber(), username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_TURN_CHANGED, campaignId,
                java.util.Map.of("battleId", battleId, "currentTurnIndex", nextIndex,
                        "currentCombatantId", nowOnTurn.getId()), user.getId());
        return toResponse(battle, combatants);
    }

    @Transactional(readOnly = true)
    public CombatantTurnResponse getCurrentTurn(UUID campaignId, UUID battleId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        Battle battle = findBattle(battleId, campaignId);
        if (battle.getStatus() != BattleStatus.ACTIVE) {
            throw new BadRequestException("Battle is not active");
        }

        List<BattleCombatant> combatants = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        if (combatants.isEmpty()) {
            throw new BadRequestException("Battle has no combatants");
        }
        BattleCombatant current = combatants.get(clampIndex(battle.getCurrentTurnIndex(), combatants.size()));

        CombatantTurnResponse.CombatantTurnResponseBuilder builder = CombatantTurnResponse.builder()
                .combatant(toCombatantResponse(current, battle));

        if (current.getType() == CombatantType.CHARACTER && current.getCharacter() != null) {
            UUID characterId = current.getCharacter().getId();
            CharacterResponse characterResponse = characterService.getCharacterById(characterId, username);
            // Surface weapon-driven attacks (equipped weapons + proficiency, incl. throw variants)
            // and progression-based class attacks alongside any manually-authored attacks so the
            // combat UI lists every actionable strike/throw/class action.
            List<CharacterAttackResponse> derived = new ArrayList<>(weaponAttackService.computeAttacks(current.getCharacter()));
            derived.addAll(classAbilityCombatService.classAttacks(current.getCharacter()));
            if (!derived.isEmpty()) {
                if (characterResponse.getAttacks() != null) {
                    derived.addAll(characterResponse.getAttacks());
                }
                characterResponse.setAttacks(derived);
            }
            SpellSlotsResponse slots = spellSlotService.getSlots(characterId, username);
            boolean hasSlots = slots != null && slots.getLevels() != null && !slots.getLevels().isEmpty();
            builder.character(characterResponse)
                    .resources(characterResourceService.getResources(characterId, username))
                    .activeEffects(characterEffectService.getActiveEffects(characterId, username))
                    .classAbilities(classAbilityCombatService.listAbilities(current.getCharacter()))
                    .spellSlots(hasSlots ? slots : null)
                    .tacticalActions(buildCharacterTacticalActions(characterResponse));
        } else if (current.getType() == CombatantType.MONSTER && current.getMonster() != null) {
            boolean gm = user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId());
            if (gm) {
                MonsterResponse monster = monsterService.getMonster(current.getMonster().getId(), username);
                builder.monster(monster)
                        .tacticalActions(buildMonsterTacticalActions(monster));
            }
        }
        return builder.build();
    }

    // ===================== Tactical targeting metadata (read-only) =====================
    // Exposes range/AoE hints for the tactical map. Spatial state stays in map-service; nothing
    // here is parsed from free localized text — unreliable data is reported as UNKNOWN.

    private List<TacticalActionResponse> buildCharacterTacticalActions(CharacterResponse character) {
        List<TacticalActionResponse> actions = new ArrayList<>();
        if (character.getAttacks() != null) {
            for (CharacterAttackResponse atk : character.getAttacks()) {
                List<TacticalActionResponse.Damage> dmg = null;
                if (atk.getDamage() != null) {
                    dmg = List.of(TacticalActionResponse.Damage.builder()
                            .dice(atk.getDamage()).damageType(atk.getDamageType()).build());
                }
                actions.add(TacticalActionResponse.builder()
                        .id(atk.getName())
                        .name(atk.getName())
                        .source(mapAttackSource(atk.getSource()))
                        .actionCost("UNKNOWN")
                        .targeting(TacticalActionResponse.Targeting.builder()
                                // The localized range string is not reliably structured, so rangeFt
                                // stays null and the shape is UNKNOWN; an attack is always single-target.
                                .mode("SINGLE_TARGET")
                                .areaShape("UNKNOWN")
                                .requiresAttackRoll(true)
                                .requiresSavingThrow(false)
                                .build())
                        .damage(dmg)
                        .build());
            }
        }
        if (character.getKnownSpells() != null && !character.getKnownSpells().isEmpty()) {
            List<UUID> spellIds = character.getKnownSpells().stream()
                    .map(CharacterKnownSpellResponse::getSpellId)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            for (Spell spell : spellRepository.findAllById(spellIds)) {
                actions.add(TacticalActionResponse.builder()
                        .id(spell.getId() != null ? spell.getId().toString() : spell.getSlug())
                        .name(spell.getNameRu())
                        .source("SPELL")
                        .actionCost(mapSpellActionCost(spell.getCastingActionSlug()))
                        .targeting(TacticalActionResponse.Targeting.builder()
                                .mode(isSelfRange(spell.getRangeType()) ? "SELF" : "UNKNOWN")
                                .rangeFt(structuredRangeFt(spell.getRangeDistance(), spell.getRangeUnit()))
                                // No structured area/radius/shape columns exist for spells — never
                                // invent them from text.
                                .areaShape("UNKNOWN")
                                .requiresAttackRoll(false)
                                .requiresSavingThrow(false)
                                .build())
                        .build());
            }
        }
        return actions;
    }

    private List<TacticalActionResponse> buildMonsterTacticalActions(MonsterResponse monster) {
        List<TacticalActionResponse> actions = new ArrayList<>();
        if (monster.getFeatures() == null) {
            return actions;
        }
        for (MonsterResponse.FeatureView f : monster.getFeatures()) {
            boolean hasAttack = f.getAttackType() != null;
            boolean hasSave = f.getSaveDc() != null;
            if (!hasAttack && !hasSave) {
                continue;
            }
            boolean requiresAttackRoll = hasAttack && f.getAttackBonus() != null;
            Short rangeShort = f.getRangeFt() != null ? f.getRangeFt() : f.getReachFt();
            Integer rangeFt = rangeShort != null ? rangeShort.intValue() : null;
            List<TacticalActionResponse.Damage> dmg = null;
            if (f.getDamages() != null && !f.getDamages().isEmpty()) {
                dmg = f.getDamages().stream()
                        .map(d -> TacticalActionResponse.Damage.builder()
                                .dice(d.getDice())
                                .damageType(d.getDamageType() != null ? d.getDamageType().getNameRusloc() : null)
                                .build())
                        .toList();
            }
            actions.add(TacticalActionResponse.builder()
                    .id(f.getId() != null ? f.getId().toString() : f.getNameRusloc())
                    .name(f.getNameRusloc())
                    .source("MONSTER_FEATURE")
                    .actionCost(mapFeatureActionCost(f.getKind()))
                    .targeting(TacticalActionResponse.Targeting.builder()
                            .mode(requiresAttackRoll ? "SINGLE_TARGET" : "UNKNOWN")
                            .rangeFt(rangeFt)
                            .areaShape("UNKNOWN")
                            .requiresAttackRoll(requiresAttackRoll)
                            .requiresSavingThrow(hasSave)
                            .build())
                    .damage(dmg)
                    .build());
        }
        return actions;
    }

    private String mapAttackSource(String source) {
        if (source == null) {
            return "MANUAL";
        }
        return switch (source.toUpperCase()) {
            case "WEAPON" -> "WEAPON";
            case "CLASS" -> "CLASS_ABILITY";
            default -> "MANUAL";
        };
    }

    private String mapSpellActionCost(String slug) {
        if (slug == null) {
            return "UNKNOWN";
        }
        String s = slug.toLowerCase();
        if (s.contains("bonus")) {
            return "BONUS_ACTION";
        }
        if (s.contains("reaction")) {
            return "REACTION";
        }
        if (s.contains("action")) {
            return "ACTION";
        }
        return "UNKNOWN";
    }

    private String mapFeatureActionCost(String kind) {
        if (kind == null) {
            return "UNKNOWN";
        }
        String k = kind.toLowerCase();
        if (k.contains("bonus")) {
            return "BONUS_ACTION";
        }
        if (k.contains("reaction")) {
            return "REACTION";
        }
        if (k.contains("action")) {
            return "ACTION";
        }
        return "UNKNOWN";
    }

    private boolean isSelfRange(String rangeType) {
        return rangeType != null && rangeType.equalsIgnoreCase("self");
    }

    /** Only surface a range in feet when the unit is reliably feet; otherwise leave it unknown (null). */
    private Integer structuredRangeFt(Integer distance, String unit) {
        if (distance == null || unit == null) {
            return null;
        }
        String u = unit.toLowerCase();
        boolean feet = u.contains("ft") || u.contains("feet") || u.contains("фут");
        return feet ? distance : null;
    }

    // ============================ Attacks & damage ============================

    /**
     * The combatant whose turn it currently is strikes a target. The attacker supplies their own
     * d20 (tabletop style); the server resolves hit/crit against the target's AC, rolls the named
     * attack's damage and applies it to the target's HP. When the target is a character the change
     * is written through to its sheet (with temp-HP absorption) so death/HP persists after the
     * battle. Authorization: the GM (any combatant, e.g. monsters) or the owner of the active
     * character, and only on that combatant's own turn.
     */
    @Transactional
    public BattleActionResultResponse performAttack(UUID campaignId, UUID battleId,
                                                    BattleAttackRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Attacks can only happen in an active battle");

        List<BattleCombatant> combatants = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        if (combatants.isEmpty()) {
            throw new BadRequestException("Battle has no combatants");
        }
        BattleCombatant attacker = combatants.get(clampIndex(battle.getCurrentTurnIndex(), combatants.size()));
        enforceControls(campaignId, user, attacker);

        // Lock the attacker row and reject a second action this turn: one action per turn is modelled,
        // multi-attack is not, so a duplicate/racing attack must not spend the action again.
        attacker = combatantRepository.findByIdForUpdate(attacker.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Attacker combatant not found"));
        if (attacker.getActionSpent() >= attacker.getActionMax()) {
            throw new BadRequestException("The action has already been used this turn");
        }

        // Lock the target row before its HP changes (matters for monster targets, whose HP lives
        // only on the combatant row; character targets are additionally locked on the sheet).
        BattleCombatant target = combatantRepository.findByIdForUpdate(request.getTargetCombatantId())
                .orElseThrow(() -> new ResourceNotFoundException("Target combatant not found"));
        if (!target.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Target does not belong to this battle");
        }
        if (target.getId().equals(attacker.getId())) {
            throw new BadRequestException("A combatant cannot attack itself");
        }

        AttackOption attack = resolveAttack(attacker, request.getAttackName());

        // Resolve the d20 according to the roll mode (virtual rolls or manual advantage dice).
        RollResolution roll = resolveAttackRoll(request);
        int effectiveD20 = roll.effectiveD20();
        AttackRollMode rollMode = request.getRollMode() != null ? request.getRollMode() : AttackRollMode.NORMAL;

        Integer damage = null;
        Integer targetAc = null;
        Integer attackBonusOut = null;
        Integer total = null;
        String outcomeName;

        if (attack.saveDc() != null) {
            // Save-based attack (e.g. a monster's breath weapon): the target rolls the supplied d20
            // against the save DC. A success halves the damage, a failure takes it in full.
            AttackResolver.SaveOutcome save = AttackResolver.resolveSave(effectiveD20, 0, attack.saveDc());
            int rolled = diceRoller.rollDamage(attack.damage(), false);
            damage = save == AttackResolver.SaveOutcome.SUCCESS ? rolled / 2 : rolled;
            if (damage > 0) {
                applyDamageOrHeal(target, -damage, user, campaignId);
            }
            outcomeName = save.name();
            total = effectiveD20;
        } else {
            targetAc = resolveTargetAc(target);
            AttackResolver.Outcome outcome = AttackResolver.resolve(effectiveD20, attack.attackBonus(), targetAc);
            if (outcome.dealsDamage()) {
                damage = diceRoller.rollDamage(attack.damage(), outcome == AttackResolver.Outcome.CRIT);
                applyDamageOrHeal(target, -damage, user, campaignId);
            }
            outcomeName = outcome.name();
            attackBonusOut = attack.attackBonus();
            total = effectiveD20 + attack.attackBonus();
        }

        attacker.setActionSpent(attacker.getActionSpent() + 1);
        combatantRepository.save(attacker);

        boolean down = target.getCurrentHp() != null && target.getCurrentHp() <= 0;
        log.info("Attack resolved: battleId={}, attacker={}, target={}, attack='{}', mode={}, d20={}, outcome={}, dmg={}, by={}",
                battleId, attacker.getDisplayName(), target.getDisplayName(), attack.name(),
                rollMode, effectiveD20, outcomeName, damage, username);

        Map<String, Object> payload = new HashMap<>();
        payload.put("battleId", battleId);
        payload.put("attackerName", attacker.getDisplayName());
        payload.put("targetName", target.getDisplayName());
        payload.put("attackName", attack.name());
        payload.put("outcome", outcomeName);
        if (damage != null) {
            payload.put("damage", damage);
        }
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_ACTION, campaignId, payload, user.getId());

        BattleResponse fresh = toResponse(battle, orderedCombatants(battleId));
        return BattleActionResultResponse.builder()
                .attackerCombatantId(attacker.getId())
                .attackerName(attacker.getDisplayName())
                .targetCombatantId(target.getId())
                .targetName(target.getDisplayName())
                .attackName(attack.name())
                .d20(effectiveD20)
                .rollMode(rollMode.name())
                .d20A(roll.d20A())
                .d20B(roll.d20B())
                .effectiveD20(effectiveD20)
                .advantageReason(request.getAdvantageReason())
                .attackBonus(attackBonusOut)
                .total(total)
                .targetAc(targetAc)
                .saveDc(attack.saveDc())
                .outcome(outcomeName)
                .damage(damage)
                .damageType(attack.damageType())
                .targetCurrentHp(target.getCurrentHp())
                .targetMaxHp(target.getMaxHp())
                .targetDown(down)
                .battle(fresh)
                .build();
    }

    /**
     * The active character consumes a carried item (e.g. drinks a healing potion). Only the
     * combatant whose turn it is may act, and only with an item they own. The item's healing dice
     * (the consumable template's damage dice, read as restoration) are rolled and applied to the
     * chosen target — by default the user themselves — then one unit of the item is spent.
     */
    @Transactional
    public BattleActionResultResponse performUseItem(UUID campaignId, UUID battleId,
                                                     BattleUseItemRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Items can only be used in an active battle");

        List<BattleCombatant> combatants = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        if (combatants.isEmpty()) {
            throw new BadRequestException("Battle has no combatants");
        }
        BattleCombatant actor = combatants.get(clampIndex(battle.getCurrentTurnIndex(), combatants.size()));
        enforceControls(campaignId, user, actor);
        if (actor.getType() != CombatantType.CHARACTER || actor.getCharacter() == null) {
            throw new BadRequestException("Only characters can use items");
        }

        // Lock the actor row and reject a second action this turn (using an item spends the action).
        actor = combatantRepository.findByIdForUpdate(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Actor combatant not found"));
        if (actor.getActionSpent() >= actor.getActionMax()) {
            throw new BadRequestException("The action has already been used this turn");
        }

        // Lock the item row so the quantity decrement / delete can't double-spend under a race.
        ItemInstance item = itemInstanceRepository.findByIdForUpdate(request.getItemInstanceId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));
        if (item.getOwnerCharacter() == null || !item.getOwnerCharacter().getId().equals(actor.getCharacter().getId())) {
            throw new BadRequestException("This item is not carried by the active character");
        }

        String healExpression = consumableHealExpression(item);
        if (healExpression == null) {
            throw new BadRequestException("This item cannot be used in combat");
        }

        BattleCombatant target = actor;
        if (request.getTargetCombatantId() != null) {
            target = combatantRepository.findByIdForUpdate(request.getTargetCombatantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Target combatant not found"));
            if (!target.getBattle().getId().equals(battleId)) {
                throw new BadRequestException("Target does not belong to this battle");
            }
        }

        int healed = diceRoller.rollDamage(healExpression, false);
        applyDamageOrHeal(target, healed, user, campaignId);
        consumeOneUnit(item);

        actor.setActionSpent(actor.getActionSpent() + 1);
        combatantRepository.save(actor);

        log.info("Item used: battleId={}, actor={}, item='{}', target={}, healed={}, by={}",
                battleId, actor.getDisplayName(), item.getDisplayName(), target.getDisplayName(), healed, username);

        Map<String, Object> payload = new HashMap<>();
        payload.put("battleId", battleId);
        payload.put("actorName", actor.getDisplayName());
        payload.put("itemName", item.getDisplayName());
        payload.put("targetName", target.getDisplayName());
        payload.put("healed", healed);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_ACTION, campaignId, payload, user.getId());

        boolean down = target.getCurrentHp() != null && target.getCurrentHp() <= 0;
        BattleResponse fresh = toResponse(battle, orderedCombatants(battleId));
        return BattleActionResultResponse.builder()
                .attackerCombatantId(actor.getId())
                .attackerName(actor.getDisplayName())
                .targetCombatantId(target.getId())
                .targetName(target.getDisplayName())
                .attackName(item.getDisplayName())
                .outcome("ITEM_USED")
                .damage(healed)
                .targetCurrentHp(target.getCurrentHp())
                .targetMaxHp(target.getMaxHp())
                .targetDown(down)
                .battle(fresh)
                .build();
    }

    /**
     * Restoration dice for a usable consumable, or {@code null} when the item has no in-combat
     * use. A legacy {@link ItemTemplate} that authors damage dice (e.g. a healing potion) exposes
     * them here as the amount restored, combined with its flat bonus.
     */
    private String consumableHealExpression(ItemInstance item) {
        ItemTemplate template = item.getTemplate();
        if (template == null) {
            return null;
        }
        String dice = template.getDamageDice();
        if (dice == null || dice.isBlank()) {
            return null;
        }
        int bonus = template.getDamageBonus() != null ? template.getDamageBonus() : 0;
        return bonus != 0 ? dice + (bonus > 0 ? "+" : "") + bonus : dice;
    }

    /** Spends one unit of an item: decrements the stack, deleting the row when it reaches zero. */
    private void consumeOneUnit(ItemInstance item) {
        int quantity = item.getQuantity() != null ? item.getQuantity() : 1;
        if (quantity <= 1) {
            itemInstanceRepository.delete(item);
        } else {
            item.setQuantity(quantity - 1);
            itemInstanceRepository.save(item);
        }
    }

    /**
     * Declaratively marks one of a combatant's action-economy slots (action / bonus action /
     * reaction) as spent for this turn. Actions and bonus actions may only be spent on the
     * combatant's own turn; a reaction can be spent at any time. Re-spending a slot is rejected.
     */
    @Transactional
    public BattleResponse spendAction(UUID campaignId, UUID battleId, UUID combatantId,
                                      SpendActionRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Actions can only be spent in an active battle");

        List<BattleCombatant> combatants = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        if (combatants.isEmpty()) {
            throw new BadRequestException("Battle has no combatants");
        }
        // Lock the combatant row so the duplicate-slot check below can't race with a concurrent spend.
        BattleCombatant combatant = combatantRepository.findByIdForUpdate(combatantId)
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found"));
        if (!combatant.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Combatant does not belong to this battle");
        }
        enforceControls(campaignId, user, combatant);

        SpendActionRequest.Slot slot = request.getSlot();
        // Actions and bonus actions are spent on the combatant's own turn. Reactions and legendary
        // actions are spent in response to other combatants, so they may be spent at any time.
        if (slot == SpendActionRequest.Slot.ACTION || slot == SpendActionRequest.Slot.BONUS_ACTION) {
            BattleCombatant active = combatants.get(clampIndex(battle.getCurrentTurnIndex(), combatants.size()));
            if (!active.getId().equals(combatant.getId())) {
                throw new BadRequestException("An action or bonus action can only be spent on the combatant's own turn");
            }
        }

        switch (slot) {
            case ACTION -> {
                if (combatant.getActionSpent() >= combatant.getActionMax()) {
                    throw new BadRequestException("The action has already been used this turn");
                }
                combatant.setActionSpent(combatant.getActionSpent() + 1);
            }
            case BONUS_ACTION -> {
                if (combatant.getBonusActionSpent() >= combatant.getBonusActionMax()) {
                    throw new BadRequestException("The bonus action has already been used this turn");
                }
                combatant.setBonusActionSpent(combatant.getBonusActionSpent() + 1);
            }
            case LEGENDARY_ACTION -> {
                if (combatant.getLegendaryActionSpent() >= combatant.getLegendaryActionMax()) {
                    throw new BadRequestException("No legendary actions remain this turn");
                }
                combatant.setLegendaryActionSpent(combatant.getLegendaryActionSpent() + 1);
            }
            case REACTION -> {
                if (Boolean.TRUE.equals(combatant.getReactionUsed())) {
                    throw new BadRequestException("The reaction has already been used this turn");
                }
                combatant.setReactionUsed(true);
            }
        }
        combatantRepository.save(combatant);

        log.info("Action economy spent: battleId={}, combatant={}, slot={}, by={}",
                battleId, combatant.getDisplayName(), slot, username);
        return toResponse(battle, orderedCombatants(battleId));
    }

    /**
     * GM manual HP adjustment of any combatant (negative {@code delta} damages, positive heals).
     * Bookkeeping for NPCs and corrections outside the attack flow; writes through to the sheet
     * for characters.
     */
    @Transactional
    public BattleResponse applyCombatantHp(UUID campaignId, UUID battleId, UUID combatantId,
                                           ApplyCombatantHpRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "HP can only be adjusted in an active battle");

        // Lock the combatant row so a manual HP edit and an attack on the same target accumulate.
        BattleCombatant combatant = combatantRepository.findByIdForUpdate(combatantId)
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found"));
        if (!combatant.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Combatant does not belong to this battle");
        }

        applyDamageOrHeal(combatant, request.getDelta(), user, campaignId);

        log.info("Combatant HP adjusted: battleId={}, combatant={}, delta={}, newHp={}, by={}",
                battleId, combatant.getDisplayName(), request.getDelta(), combatant.getCurrentHp(), username);
        Map<String, Object> payload = new HashMap<>();
        payload.put("battleId", battleId);
        payload.put("targetName", combatant.getDisplayName());
        payload.put("delta", request.getDelta());
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_ACTION, campaignId, payload, user.getId());

        return toResponse(battle, orderedCombatants(battleId));
    }

    /**
     * GM adjustment of a combatant's action-economy maxima (action / bonus action / legendary
     * action). Models the pools growing with level or spells and grants legendary actions. Only the
     * provided fields change; spent counters are clamped so they never exceed the new maximum.
     */
    @Transactional
    public BattleResponse adjustActionEconomy(UUID campaignId, UUID battleId, UUID combatantId,
                                              AdjustActionEconomyRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Battle battle = findBattleForUpdate(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Action economy can only be adjusted in an active battle");

        BattleCombatant combatant = combatantRepository.findByIdForUpdate(combatantId)
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found"));
        if (!combatant.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Combatant does not belong to this battle");
        }

        if (request.getActionMax() != null) {
            combatant.setActionMax(request.getActionMax());
            combatant.setActionSpent(Math.min(nz(combatant.getActionSpent()), request.getActionMax()));
        }
        if (request.getBonusActionMax() != null) {
            combatant.setBonusActionMax(request.getBonusActionMax());
            combatant.setBonusActionSpent(Math.min(nz(combatant.getBonusActionSpent()), request.getBonusActionMax()));
        }
        if (request.getLegendaryActionMax() != null) {
            combatant.setLegendaryActionMax(request.getLegendaryActionMax());
            combatant.setLegendaryActionSpent(
                    Math.min(nz(combatant.getLegendaryActionSpent()), request.getLegendaryActionMax()));
        }
        combatantRepository.save(combatant);

        log.info("Action economy adjusted: battleId={}, combatant={}, action={}, bonus={}, legendary={}, by={}",
                battleId, combatant.getDisplayName(), combatant.getActionMax(),
                combatant.getBonusActionMax(), combatant.getLegendaryActionMax(), username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_ACTION, campaignId,
                Map.of("battleId", battleId, "combatantId", combatantId), user.getId());
        return toResponse(battle, orderedCombatants(battleId));
    }

    /**
     * A combatant's usable attack reduced to the numbers the resolver needs. {@code saveDc} is set
     * only for save-based attacks (the target rolls a saving throw instead of the attacker rolling
     * to hit); it stays null for ordinary attack-roll strikes.
     */
    private record AttackOption(String name, int attackBonus, String damage, String damageType, Integer saveDc) {
    }

    /** The two dice considered and the single die selected for the attack per its roll mode. */
    private record RollResolution(Integer d20A, Integer d20B, int effectiveD20) {
    }

    /**
     * Turns the request's roll mode and dice into the effective d20. Manual dice for
     * ADVANTAGE/DISADVANTAGE must come as a {@code d20A}/{@code d20B} pair (the server keeps the
     * higher / lower); a lone legacy {@code d20} is accepted only for NORMAL. When no dice are
     * supplied the server rolls virtually: one die for NORMAL, two (keep higher/lower) otherwise.
     * Policy: advantage/disadvantage with a single manual value is rejected for strictness.
     */
    private RollResolution resolveAttackRoll(BattleAttackRequest request) {
        AttackRollMode mode = request.getRollMode() != null ? request.getRollMode() : AttackRollMode.NORMAL;
        Integer a = request.getD20A();
        Integer b = request.getD20B();
        Integer single = request.getD20();

        if ((a == null) != (b == null)) {
            throw new BadRequestException("Provide both d20A and d20B together, or neither");
        }
        boolean hasPair = a != null;

        if (mode == AttackRollMode.NORMAL) {
            if (hasPair) {
                throw new BadRequestException("A NORMAL roll expects a single d20, not a d20A/d20B pair");
            }
            int value = single != null ? single : diceRoller.rollD20();
            return new RollResolution(value, null, value);
        }

        // ADVANTAGE / DISADVANTAGE
        if (!hasPair) {
            if (single != null) {
                throw new BadRequestException(mode + " requires both d20A and d20B (two dice)");
            }
            a = diceRoller.rollD20();
            b = diceRoller.rollD20();
        }
        int effective = mode == AttackRollMode.ADVANTAGE ? Math.max(a, b) : Math.min(a, b);
        return new RollResolution(a, b, effective);
    }

    /**
     * A character's full attack list: weapon-driven attacks (incl. throw/two-handed variants),
     * progression-based class attacks (class features that deal damage) and any manually-authored
     * attacks from the sheet.
     */
    private List<CharacterAttackResponse> characterAttackList(PlayerCharacter character) {
        List<CharacterAttackResponse> list = new ArrayList<>(weaponAttackService.computeAttacks(character));
        list.addAll(classAbilityCombatService.classAttacks(character));
        String json = character.getAttacksJson();
        if (json != null && !json.isBlank()) {
            try {
                list.addAll(objectMapper.readValue(json, new TypeReference<List<CharacterAttackResponse>>() {}));
            } catch (Exception e) {
                throw new BadRequestException("Could not read this character's attacks", e);
            }
        }
        return list;
    }

    /** Finds the named attack on the active combatant — character weapon/json attacks or monster features. */
    private AttackOption resolveAttack(BattleCombatant attacker, String attackName) {
        if (attacker.getType() == CombatantType.CHARACTER && attacker.getCharacter() != null) {
            List<CharacterAttackResponse> attacks = characterAttackList(attacker.getCharacter());
            if (attacks.isEmpty()) {
                throw new BadRequestException("This character has no attacks");
            }
            return attacks.stream()
                    .filter(a -> a.getName() != null && a.getName().equalsIgnoreCase(attackName))
                    .findFirst()
                    .map(a -> new AttackOption(a.getName(),
                            AttackResolver.parseAttackBonus(a.getAttackBonus()),
                            a.getDamage(), a.getDamageType(), null))
                    .orElseThrow(() -> new BadRequestException("Attack '" + attackName + "' not found on this character"));
        }

        if (attacker.getType() == CombatantType.MONSTER && attacker.getMonster() != null) {
            MonsterFeature feature = attacker.getMonster().getFeatures().stream()
                    .filter(f -> f.getAttackType() != null && f.getNameRusloc() != null
                            && f.getNameRusloc().equalsIgnoreCase(attackName))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Attack '" + attackName + "' not found on this monster"));
            int bonus = feature.getAttackBonus() != null ? feature.getAttackBonus() : 0;
            Optional<FeatureDamage> primary = feature.getDamages().stream()
                    .min(Comparator.comparing(FeatureDamage::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())));
            String dice = primary.map(FeatureDamage::getDice)
                    .filter(d -> d != null && !d.isBlank())
                    .orElse(null);
            String damageType = primary
                    .map(d -> d.getDamageType() != null ? d.getDamageType().getNameRusloc() : null)
                    .orElse(null);
            // Weapon attacks store their damage inline in the description, not in structured
            // feature_damages rows — fall back to parsing the dice out of the text.
            if (dice == null) {
                dice = AttackResolver.extractDamageExpression(feature.getDescriptionRusloc());
            }
            Integer saveDc = feature.getSaveDc() != null ? feature.getSaveDc().intValue() : null;
            return new AttackOption(feature.getNameRusloc(), bonus, dice, damageType, saveDc);
        }

        throw new BadRequestException("This combatant cannot attack");
    }

    /** Target armor class: monster's authored AC or character's AC; falls back to 10. */
    private int resolveTargetAc(BattleCombatant target) {
        if (target.getType() == CombatantType.MONSTER && target.getMonster() != null
                && target.getMonster().getArmorClass() != null) {
            return target.getMonster().getArmorClass().intValue();
        }
        if (target.getType() == CombatantType.CHARACTER && target.getCharacter() != null
                && target.getCharacter().getArmorClass() != null) {
            return target.getCharacter().getArmorClass();
        }
        return 10;
    }

    /**
     * Applies a signed HP {@code delta} to a combatant. For characters the change is written
     * through to the sheet (temp HP absorbs damage first, healing is capped at max HP) and an
     * {@code HP_CHANGED} event keeps the character views in sync; the combatant row mirrors the
     * result. Monsters are tracked solely on the combatant row.
     */
    private void applyDamageOrHeal(BattleCombatant combatant, int delta, User actor, UUID campaignId) {
        if (combatant.getType() == CombatantType.CHARACTER && combatant.getCharacter() != null) {
            // Re-load under a pessimistic write lock so simultaneous attacks / manual HP edits on the
            // same character accumulate instead of overwriting one another (deltas, not last-write-wins).
            PlayerCharacter character = characterRepository.findByIdForUpdate(combatant.getCharacter().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
            int maxHp = character.getMaxHp() != null ? character.getMaxHp()
                    : (combatant.getMaxHp() != null ? combatant.getMaxHp() : 0);

            character.applyHpDelta(delta, maxHp);
            characterRepository.save(character);

            int currentHp = character.getCurrentHp();
            int tempHp = character.getTempHp();
            combatant.setCurrentHp(currentHp);
            combatant.setMaxHp(maxHp > 0 ? maxHp : combatant.getMaxHp());
            combatantRepository.save(combatant);

            webSocketEventService.sendCampaignEvent(WebSocketEventType.HP_CHANGED, campaignId,
                    character.getId(),
                    Map.of("currentHp", currentHp, "tempHp", tempHp, "maxHp", maxHp),
                    actor.getId());
        } else {
            int maxHp = combatant.getMaxHp() != null ? combatant.getMaxHp() : 0;
            int currentHp = combatant.getCurrentHp() != null ? combatant.getCurrentHp() : 0;
            if (delta < 0) {
                currentHp = Math.max(0, currentHp + delta);
            } else if (delta > 0) {
                currentHp = maxHp > 0 ? Math.min(maxHp, currentHp + delta) : currentHp + delta;
            }
            combatant.setCurrentHp(currentHp);
            combatantRepository.save(combatant);
        }
    }

    // ================================ Helpers ================================

    /** The GM/admin may act for any combatant; a player only for their own active character. */
    private void enforceControls(UUID campaignId, User user, BattleCombatant combatant) {
        if (user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId())) {
            return;
        }
        if (combatant.getType() == CombatantType.CHARACTER && combatant.getCharacter() != null
                && combatant.getCharacter().getOwner().getId().equals(user.getId())) {
            return;
        }
        throw new AccessDeniedException("Only the GM or the active character's owner can act for this combatant");
    }

    private void enforceCanEndTurn(UUID campaignId, User user, BattleCombatant current) {
        if (user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId())) {
            return;
        }
        if (current.getType() == CombatantType.CHARACTER && current.getCharacter() != null
                && current.getCharacter().getOwner().getId().equals(user.getId())) {
            return;
        }
        throw new AccessDeniedException("Only the GM or the active character's owner can end this turn");
    }

    private UUID activeCombatantId(Battle battle, List<BattleCombatant> ordered) {
        if (battle.getStatus() != BattleStatus.ACTIVE || ordered.isEmpty()) {
            return null;
        }
        return ordered.get(clampIndex(battle.getCurrentTurnIndex(), ordered.size())).getId();
    }

    private int dexValue(PlayerCharacter character) {
        return character.getStats().stream()
                .filter(s -> s.getStatType() != null && DEX_CODE.equals(s.getStatType().getSlug()))
                .findFirst()
                .map(CharacterStat::getValue)
                .orElse(10);
    }

    private int dexBuffBonus(PlayerCharacter character) {
        int total = 0;
        for (CharacterActiveEffect effect : character.getActiveEffects()) {
            BuffDebuff bd = effect.getBuffDebuff();
            if (bd != null && "STAT_MODIFIER".equals(bd.getEffectType())
                    && bd.getTargetStat() != null && DEX_CODE.equals(bd.getTargetStat().getSlug())
                    && bd.getModifierValue() != null) {
                total += Boolean.TRUE.equals(bd.getIsBuff()) ? bd.getModifierValue() : -bd.getModifierValue();
            }
        }
        return total;
    }

    private List<BattleCombatant> orderedCombatants(UUID battleId) {
        List<BattleCombatant> list = new ArrayList<>(combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId));
        list.sort(Comparator
                .comparing(BattleCombatant::getTurnOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(c -> c.getCreatedAt() == null ? Instant.EPOCH : c.getCreatedAt()));
        return list;
    }

    private static int clampIndex(Integer index, int size) {
        int i = index == null ? 0 : index;
        if (i < 0) return 0;
        if (i >= size) return size - 1;
        return i;
    }

    private void requireStatus(Battle battle, BattleStatus expected, String message) {
        if (battle.getStatus() != expected) {
            throw new BadRequestException(message);
        }
    }

    private Battle findBattle(UUID battleId, UUID campaignId) {
        return battleRepository.findByIdAndCampaignId(battleId, campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Battle not found"));
    }

    /**
     * Row-locking battle load (SELECT ... FOR UPDATE) for every mutating combat action. It both
     * serializes concurrent mutations on the same battle and fixes the lock order (battle first,
     * then combatant/item rows) so the per-combatant locks below cannot deadlock.
     */
    private Battle findBattleForUpdate(UUID battleId, UUID campaignId) {
        return battleRepository.findByIdAndCampaignIdForUpdate(battleId, campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Battle not found"));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // ====================== Map-service integration contracts ======================

    /**
     * Read-only projection of what a given user may do in a battle, for service-to-service callers
     * (map-service) that must authorize token control without touching the core DB or duplicating
     * combat permission rules. GM/admin manage the battle and control any combatant; a player can
     * only control their own character combatants; a non-member sees {@code canView=false}.
     */
    @Transactional(readOnly = true)
    public BattleAccessResponse getBattleAccess(UUID campaignId, UUID battleId, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        campaignService.findCampaign(campaignId);
        findBattle(battleId, campaignId);

        boolean gm = user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, userId);
        boolean canView = gm || campaignService.isMemberOfCampaign(campaignId, userId);

        List<UUID> controllableCombatantIds = new ArrayList<>();
        List<UUID> controllableCharacterIds = new ArrayList<>();
        if (canView) {
            for (BattleCombatant c : orderedCombatants(battleId)) {
                boolean owns = c.getType() == CombatantType.CHARACTER && c.getCharacter() != null
                        && c.getCharacter().getOwner() != null
                        && c.getCharacter().getOwner().getId().equals(userId);
                if (gm || owns) {
                    controllableCombatantIds.add(c.getId());
                    if (c.getType() == CombatantType.CHARACTER && c.getCharacter() != null) {
                        controllableCharacterIds.add(c.getCharacter().getId());
                    }
                }
            }
        }

        return BattleAccessResponse.builder()
                .battleId(battleId)
                .campaignId(campaignId)
                .userId(userId)
                .canView(canView)
                .canManageBattle(gm)
                .canControlAnyCombatant(gm)
                .controllableCombatantIds(controllableCombatantIds)
                .controllableCharacterIds(controllableCharacterIds)
                .build();
    }

    /**
     * Minimal, safe identity of one combatant for map-service to create a token-combat link from.
     * Deliberately omits private character-sheet data; token footprint defaults to 1x1 because
     * spatial sizing is owned by map-service, not core BE.
     */
    @Transactional(readOnly = true)
    public CombatantReferenceResponse getCombatantReference(UUID campaignId, UUID battleId, UUID combatantId) {
        campaignService.findCampaign(campaignId);
        Battle battle = findBattle(battleId, campaignId);
        BattleCombatant c = combatantRepository.findById(combatantId)
                .orElseThrow(() -> new ResourceNotFoundException("Combatant not found"));
        if (!c.getBattle().getId().equals(battleId)) {
            throw new ResourceNotFoundException("Combatant does not belong to this battle");
        }

        return CombatantReferenceResponse.builder()
                .battleId(battleId)
                .campaignId(campaignId)
                .combatantId(c.getId())
                .type(c.getType().name())
                .displayName(c.getDisplayName())
                .characterId(c.getCharacter() != null ? c.getCharacter().getId() : null)
                .monsterId(c.getMonster() != null ? c.getMonster().getId() : null)
                .ownerUserId(c.getCharacter() != null && c.getCharacter().getOwner() != null
                        ? c.getCharacter().getOwner().getId() : null)
                .currentHp(c.getCurrentHp())
                .maxHp(c.getMaxHp())
                .turnOrder(c.getTurnOrder())
                .currentTurn(toCombatantResponse(c, battle).isCurrentTurn())
                .widthCells(1)
                .heightCells(1)
                .build();
    }

    // ================================ Mapping ================================

    private BattleResponse toResponse(Battle battle, List<BattleCombatant> combatants) {
        List<BigDecimal> crValues = combatants.stream()
                .filter(c -> c.getType() == CombatantType.MONSTER && c.getMonster() != null)
                .map(c -> c.getMonster().getCrValue())
                .toList();
        List<Integer> xpBases = combatants.stream()
                .filter(c -> c.getType() == CombatantType.MONSTER && c.getMonster() != null)
                .map(c -> c.getMonster().getXpBase())
                .toList();
        int monsterCount = (int) combatants.stream().filter(c -> c.getType() == CombatantType.MONSTER).count();

        UUID currentCombatantId = null;
        if (battle.getStatus() == BattleStatus.ACTIVE && !combatants.isEmpty()) {
            currentCombatantId = combatants.get(clampIndex(battle.getCurrentTurnIndex(), combatants.size())).getId();
        }
        final UUID activeId = currentCombatantId;

        List<BattleCombatantResponse> combatantResponses = combatants.stream()
                .map(c -> toCombatantResponse(c, battle, activeId))
                .toList();

        return BattleResponse.builder()
                .id(battle.getId())
                .campaignId(battle.getCampaign().getId())
                .name(battle.getName())
                .status(battle.getStatus().name())
                .roundNumber(battle.getRoundNumber())
                .currentTurnIndex(battle.getCurrentTurnIndex())
                .currentCombatantId(currentCombatantId)
                .monsterCount(monsterCount)
                .averageDanger(CombatCalculator.averageDanger(crValues))
                .totalXp(CombatCalculator.totalXp(xpBases, battle.getOverrideXp()))
                .overrideXp(battle.getOverrideXp())
                .combatants(combatantResponses)
                .startedAt(battle.getStartedAt())
                .endedAt(battle.getEndedAt())
                .createdAt(battle.getCreatedAt())
                .updatedAt(battle.getUpdatedAt())
                .build();
    }

    private BattleCombatantResponse toCombatantResponse(BattleCombatant c, Battle battle) {
        UUID activeId = null;
        if (battle.getStatus() == BattleStatus.ACTIVE && c.getTurnOrder() != null
                && c.getTurnOrder().equals(battle.getCurrentTurnIndex())) {
            activeId = c.getId();
        }
        return toCombatantResponse(c, battle, activeId);
    }

    private BattleCombatantResponse toCombatantResponse(BattleCombatant c, Battle battle, UUID activeCombatantId) {
        boolean currentTurn = battle.getStatus() == BattleStatus.ACTIVE
                && activeCombatantId != null && activeCombatantId.equals(c.getId());
        return BattleCombatantResponse.builder()
                .id(c.getId())
                .type(c.getType().name())
                .displayName(c.getDisplayName())
                .monsterId(c.getMonster() != null ? c.getMonster().getId() : null)
                .characterId(c.getCharacter() != null ? c.getCharacter().getId() : null)
                .ownerUserId(c.getCharacter() != null && c.getCharacter().getOwner() != null
                        ? c.getCharacter().getOwner().getId() : null)
                .instanceIndex(c.getInstanceIndex())
                .initiative(c.getInitiative())
                .initiativeRoll(c.getInitiativeRoll())
                .turnOrder(c.getTurnOrder())
                .currentHp(c.getCurrentHp())
                .maxHp(c.getMaxHp())
                .currentTurn(currentTurn)
                .actionMax(nz(c.getActionMax()))
                .actionSpent(nz(c.getActionSpent()))
                .bonusActionMax(nz(c.getBonusActionMax()))
                .bonusActionSpent(nz(c.getBonusActionSpent()))
                .legendaryActionMax(nz(c.getLegendaryActionMax()))
                .legendaryActionSpent(nz(c.getLegendaryActionSpent()))
                .reactionUsed(Boolean.TRUE.equals(c.getReactionUsed()))
                .build();
    }

    private static int nz(Integer value) {
        return value != null ? value : 0;
    }

    /** Clears a combatant's spent action economy (action / bonus / legendary) at the start of their turn. */
    private void resetActionEconomy(BattleCombatant combatant) {
        combatant.setActionSpent(0);
        combatant.setBonusActionSpent(0);
        combatant.setLegendaryActionSpent(0);
        combatant.setReactionUsed(false);
        combatantRepository.save(combatant);
    }
}
