package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.BattleStatus;
import com.dnd.app.domain.enums.CombatantType;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.request.AddBattleMonstersRequest;
import com.dnd.app.dto.request.ApplyCombatantHpRequest;
import com.dnd.app.dto.request.BattleAttackRequest;
import com.dnd.app.dto.request.CreateBattleRequest;
import com.dnd.app.dto.request.JoinBattleRequest;
import com.dnd.app.dto.request.UpdateBattleXpRequest;
import com.dnd.app.dto.response.*;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import com.dnd.app.service.combat.AttackResolver;
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
        Battle battle = findBattle(battleId, campaignId);
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
        Battle battle = findBattle(battleId, campaignId);
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
        Battle battle = findBattle(battleId, campaignId);
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

        log.info("Turn passed: battleId={}, newIndex={}, round={}, by={}",
                battleId, nextIndex, battle.getRoundNumber(), username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_TURN_CHANGED, campaignId,
                java.util.Map.of("battleId", battleId, "currentTurnIndex", nextIndex), user.getId());
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
            // Surface weapon-driven attacks (derived from equipped weapons + proficiency) alongside
            // any manually-authored attacks so the combat UI lists actionable strikes/throws.
            List<CharacterAttackResponse> weaponAttacks = weaponAttackService.computeAttacks(current.getCharacter());
            if (!weaponAttacks.isEmpty()) {
                List<CharacterAttackResponse> combined = new ArrayList<>(weaponAttacks);
                if (characterResponse.getAttacks() != null) {
                    combined.addAll(characterResponse.getAttacks());
                }
                characterResponse.setAttacks(combined);
            }
            builder.character(characterResponse)
                    .resources(characterResourceService.getResources(characterId, username))
                    .activeEffects(characterEffectService.getActiveEffects(characterId, username));
        } else if (current.getType() == CombatantType.MONSTER && current.getMonster() != null) {
            boolean gm = user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId());
            if (gm) {
                builder.monster(monsterService.getMonster(current.getMonster().getId(), username));
            }
        }
        return builder.build();
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
        Battle battle = findBattle(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "Attacks can only happen in an active battle");

        List<BattleCombatant> combatants = combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId);
        if (combatants.isEmpty()) {
            throw new BadRequestException("Battle has no combatants");
        }
        BattleCombatant attacker = combatants.get(clampIndex(battle.getCurrentTurnIndex(), combatants.size()));
        enforceControls(campaignId, user, attacker);

        BattleCombatant target = combatantRepository.findById(request.getTargetCombatantId())
                .orElseThrow(() -> new ResourceNotFoundException("Target combatant not found"));
        if (!target.getBattle().getId().equals(battleId)) {
            throw new BadRequestException("Target does not belong to this battle");
        }
        if (target.getId().equals(attacker.getId())) {
            throw new BadRequestException("A combatant cannot attack itself");
        }

        AttackOption attack = resolveAttack(attacker, request.getAttackName());
        int targetAc = resolveTargetAc(target);
        AttackResolver.Outcome outcome = AttackResolver.resolve(request.getD20(), attack.attackBonus(), targetAc);

        Integer damage = null;
        if (outcome.dealsDamage()) {
            damage = diceRoller.rollDamage(attack.damage(), outcome == AttackResolver.Outcome.CRIT);
            applyDamageOrHeal(target, -damage, user, campaignId);
        }

        boolean down = target.getCurrentHp() != null && target.getCurrentHp() <= 0;
        log.info("Attack resolved: battleId={}, attacker={}, target={}, attack='{}', d20={}, outcome={}, dmg={}, by={}",
                battleId, attacker.getDisplayName(), target.getDisplayName(), attack.name(),
                request.getD20(), outcome, damage, username);

        Map<String, Object> payload = new HashMap<>();
        payload.put("battleId", battleId);
        payload.put("attackerName", attacker.getDisplayName());
        payload.put("targetName", target.getDisplayName());
        payload.put("attackName", attack.name());
        payload.put("outcome", outcome.name());
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
                .d20(request.getD20())
                .attackBonus(attack.attackBonus())
                .total(request.getD20() + attack.attackBonus())
                .targetAc(targetAc)
                .outcome(outcome.name())
                .damage(damage)
                .damageType(attack.damageType())
                .targetCurrentHp(target.getCurrentHp())
                .targetMaxHp(target.getMaxHp())
                .targetDown(down)
                .battle(fresh)
                .build();
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
        Battle battle = findBattle(battleId, campaignId);
        requireStatus(battle, BattleStatus.ACTIVE, "HP can only be adjusted in an active battle");

        BattleCombatant combatant = combatantRepository.findById(combatantId)
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

    /** A combatant's usable attack reduced to the numbers the resolver needs. */
    private record AttackOption(String name, int attackBonus, String damage, String damageType) {
    }

    /** A character's full attack list: weapon-driven attacks plus any manually-authored ones. */
    private List<CharacterAttackResponse> characterAttackList(PlayerCharacter character) {
        List<CharacterAttackResponse> list = new ArrayList<>(weaponAttackService.computeAttacks(character));
        String json = character.getAttacksJson();
        if (json != null && !json.isBlank()) {
            try {
                list.addAll(objectMapper.readValue(json, new TypeReference<List<CharacterAttackResponse>>() {}));
            } catch (Exception e) {
                throw new BadRequestException("Could not read this character's attacks");
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
                            a.getDamage(), a.getDamageType()))
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
            return new AttackOption(feature.getNameRusloc(), bonus, dice, damageType);
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
            PlayerCharacter character = combatant.getCharacter();
            int maxHp = character.getMaxHp() != null ? character.getMaxHp()
                    : (combatant.getMaxHp() != null ? combatant.getMaxHp() : 0);
            int currentHp = character.getCurrentHp() != null ? character.getCurrentHp() : 0;
            int tempHp = character.getTempHp() != null ? character.getTempHp() : 0;

            if (delta < 0) {
                int dmg = -delta;
                int absorbed = Math.min(tempHp, dmg);
                tempHp -= absorbed;
                currentHp = Math.max(0, currentHp - (dmg - absorbed));
            } else if (delta > 0) {
                currentHp = maxHp > 0 ? Math.min(currentHp + delta, maxHp) : currentHp + delta;
            }

            character.setCurrentHp(currentHp);
            character.setTempHp(tempHp);
            characterRepository.save(character);

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

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
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
                .build();
    }
}
