package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.CampaignRole;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.combat.ModifierTarget;
import com.dnd.app.dto.request.ApplyEffectRequest;
import com.dnd.app.dto.response.AbilityCheckResponse;
import com.dnd.app.dto.response.CharacterActiveEffectResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.util.AbilityScores;
import com.dnd.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterEffectService {

    private final CharacterActiveEffectRepository characterActiveEffectRepository;
    private final BuffDebuffRepository buffDebuffRepository;
    private final PlayerCharacterRepository playerCharacterRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final CampaignMemberRepository campaignMemberRepository;
    private final WebSocketEventService webSocketEventService;
    private final ModifierAggregator modifierAggregator;

    @Transactional
    public CharacterActiveEffectResponse applyEffect(UUID campaignId, UUID characterId,
                                                      ApplyEffectRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);

        PlayerCharacter character = findCharacter(characterId);
        if (character.getCampaign() == null || !character.getCampaign().getId().equals(campaignId)) {
            throw new ResourceNotFoundException("Character does not belong to this campaign");
        }
        BuffDebuff buffDebuff = buffDebuffRepository.findById(request.getBuffDebuffId())
                .orElseThrow(() -> new ResourceNotFoundException("BuffDebuff not found"));

        CharacterActiveEffect effect = CharacterActiveEffect.builder()
                .character(character)
                .buffDebuff(buffDebuff)
                .appliedBy(user)
                .remainingRounds(request.getRemainingRounds())
                .build();
        effect = characterActiveEffectRepository.save(effect);

        log.info("Effect applied: effectId={}, characterId={}, buffDebuffId={}, by={}",
                effect.getId(), characterId, request.getBuffDebuffId(), username);
        CharacterActiveEffectResponse response = toResponse(effect);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BUFF_APPLIED, campaignId,
                characterId, response, user.getId());
        return response;
    }

    @Transactional
    public void removeEffect(UUID campaignId, UUID characterId, UUID effectId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);

        CharacterActiveEffect effect = characterActiveEffectRepository.findById(effectId)
                .orElseThrow(() -> new ResourceNotFoundException("Active effect not found"));

        if (!effect.getCharacter().getId().equals(characterId)) {
            throw new ResourceNotFoundException("Effect does not belong to this character");
        }
        Campaign characterCampaign = effect.getCharacter().getCampaign();
        if (characterCampaign == null || !characterCampaign.getId().equals(campaignId)) {
            throw new ResourceNotFoundException("Character does not belong to this campaign");
        }

        characterActiveEffectRepository.delete(effect);
        log.info("Effect removed: effectId={}, characterId={}, by={}", effectId, characterId, username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BUFF_REMOVED, campaignId,
                characterId, Map.of("effectId", effectId), user.getId());
    }

    @Transactional(readOnly = true)
    public List<CharacterActiveEffectResponse> getActiveEffects(UUID characterId, String username) {
        User user = getUser(username);
        PlayerCharacter character = findCharacter(characterId);

        // Any member of the same campaign can view
        if (user.getRole() != Role.ADMIN) {
            if (character.getCampaign() == null) {
                // Only the owner can view if no campaign
                if (!character.getOwner().getId().equals(user.getId())) {
                    throw new AccessDeniedException("You cannot view effects for this character");
                }
            } else {
                if (!campaignService.isMemberOfCampaign(character.getCampaign().getId(), user.getId())) {
                    throw new AccessDeniedException("You are not a member of this character's campaign");
                }
            }
        }

        return characterActiveEffectRepository.findByCharacterId(characterId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AbilityCheckResponse calculateAbilityCheckModifier(UUID characterId, UUID statTypeId, String username) {
        User user = getUser(username);
        PlayerCharacter character = findCharacter(characterId);

        if (user.getRole() != Role.ADMIN) {
            if (character.getCampaign() != null
                    && !campaignService.isMemberOfCampaign(character.getCampaign().getId(), user.getId())) {
                throw new AccessDeniedException("You are not a member of this character's campaign");
            }
        }

        // Find base stat value
        int baseValue = character.getStats().stream()
                .filter(s -> s.getStatType().getId().equals(statTypeId))
                .findFirst()
                .map(CharacterStat::getValue)
                .orElse(10);

        int baseModifier = AbilityScores.modifier(baseValue);

        // Sum of active STAT_MODIFIER buffs/debuffs on that stat
        List<CharacterActiveEffect> activeEffects = characterActiveEffectRepository.findByCharacterId(characterId);
        int buffTotal = 0;
        int debuffTotal = 0;
        for (CharacterActiveEffect effect : activeEffects) {
            BuffDebuff bd = effect.getBuffDebuff();
            if ("STAT_MODIFIER".equals(bd.getEffectType())
                    && bd.getTargetStat() != null
                    && bd.getTargetStat().getId().equals(statTypeId)
                    && bd.getModifierValue() != null) {
                if (Boolean.TRUE.equals(bd.getIsBuff())) {
                    buffTotal += bd.getModifierValue();
                } else {
                    debuffTotal += bd.getModifierValue();
                }
            }
        }

        String statSlug = character.getStats().stream()
                .filter(s -> s.getStatType().getId().equals(statTypeId))
                .findFirst()
                .map(s -> s.getStatType().getSlug())
                .orElse(null);
        // Feature-effect contributions (formula-evaluated) added on top of the legacy buff/debuff sum.
        int featureBonus = modifierAggregator.featureTotal(characterId, ModifierTarget.statCheck(statTypeId, statSlug));
        int buffBonus = buffTotal - debuffTotal + featureBonus;
        int totalModifier = baseModifier + buffBonus;

        String statName = character.getStats().stream()
                .filter(s -> s.getStatType().getId().equals(statTypeId))
                .findFirst()
                .map(s -> s.getStatType().getNameRu())
                .orElse("Unknown");

        return AbilityCheckResponse.builder()
                .statName(statName)
                .baseValue(baseValue)
                .modifier(baseModifier)
                .buffBonus(buffBonus)
                .equipmentBonus(0)
                .totalModifier(totalModifier)
                .build();
    }

    @Transactional
    public void decrementRounds(UUID characterId) {
        List<CharacterActiveEffect> effects = characterActiveEffectRepository.findByCharacterId(characterId);
        for (CharacterActiveEffect effect : effects) {
            if (effect.getRemainingRounds() != null) {
                int newRounds = effect.getRemainingRounds() - 1;
                if (newRounds <= 0) {
                    characterActiveEffectRepository.delete(effect);
                    log.info("Effect expired and removed: effectId={}, characterId={}",
                            effect.getId(), characterId);
                } else {
                    effect.setRemainingRounds(newRounds);
                    characterActiveEffectRepository.save(effect);
                }
            }
        }
    }

    // --- Private helpers ---

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private PlayerCharacter findCharacter(UUID characterId) {
        return playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
    }

    private CharacterActiveEffectResponse toResponse(CharacterActiveEffect effect) {
        return CharacterActiveEffectResponse.builder()
                .id(effect.getId())
                .buffDebuffId(effect.getBuffDebuff().getId())
                .buffDebuffName(effect.getBuffDebuff().getName())
                .effectType(effect.getBuffDebuff().getEffectType())
                .isBuff(effect.getBuffDebuff().getIsBuff())
                .modifierValue(effect.getBuffDebuff().getModifierValue())
                .targetStatName(effect.getBuffDebuff().getTargetStat() != null
                        ? effect.getBuffDebuff().getTargetStat().getNameRu() : null)
                .remainingRounds(effect.getRemainingRounds())
                .appliedByUsername(effect.getAppliedBy().getUsername())
                .appliedAt(effect.getAppliedAt())
                .build();
    }
}
