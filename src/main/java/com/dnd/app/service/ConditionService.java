package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.AddConditionModifierRequest;
import com.dnd.app.dto.request.ApplyConditionRequest;
import com.dnd.app.dto.request.CreateConditionRequest;
import com.dnd.app.dto.response.CharacterConditionResponse;
import com.dnd.app.dto.response.ConditionModifierResponse;
import com.dnd.app.dto.response.ConditionResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConditionService {

    private final ConditionRepository conditionRepository;
    private final ConditionModifierRepository modifierRepository;
    private final CharacterConditionRepository charCondRepository;
    private final StatTypeRepository statTypeRepository;
    private final PlayerCharacterRepository characterRepository;
    private final UserRepository userRepository;

    @Transactional
    public ConditionResponse createCondition(CreateConditionRequest request, String username) {
        User gm = getGM(username);
        if (conditionRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Condition name already exists");
        }
        Condition condition = Condition.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(gm)
                .build();
        condition = conditionRepository.save(condition);
        return toResponse(condition);
    }

    @Transactional(readOnly = true)
    public List<ConditionResponse> listConditions(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<Condition> conditions;
        if (user.getRole() == Role.ADMIN) {
            conditions = conditionRepository.findAll();
        } else if (user.getRole() == Role.GAME_MASTER) {
            conditions = conditionRepository.findAllByCreatedById(user.getId());
        } else {
            throw new AccessDeniedException("Players cannot list conditions");
        }
        return conditions.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ConditionResponse getCondition(UUID id, String username) {
        Condition condition = conditionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Condition not found"));
        return toResponse(condition);
    }

    @Transactional
    public ConditionResponse updateCondition(UUID id, CreateConditionRequest request, String username) {
        User gm = getGM(username);
        Condition condition = conditionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Condition not found"));
        if (!condition.getCreatedBy().getId().equals(gm.getId())) {
            throw new AccessDeniedException("You did not create this condition");
        }
        if (!condition.getName().equals(request.getName()) && conditionRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Condition name already exists");
        }
        condition.setName(request.getName());
        condition.setDescription(request.getDescription());
        condition = conditionRepository.save(condition);
        return toResponse(condition);
    }

    @Transactional
    public void deleteCondition(UUID id, String username) {
        User gm = getGM(username);
        Condition condition = conditionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Condition not found"));
        if (!condition.getCreatedBy().getId().equals(gm.getId())) {
            throw new AccessDeniedException("You did not create this condition");
        }
        conditionRepository.delete(condition);
    }

    @Transactional
    public ConditionResponse addModifier(UUID conditionId, AddConditionModifierRequest request, String username) {
        User gm = getGM(username);
        Condition condition = conditionRepository.findById(conditionId)
                .orElseThrow(() -> new ResourceNotFoundException("Condition not found"));
        if (!condition.getCreatedBy().getId().equals(gm.getId())) {
            throw new AccessDeniedException("You did not create this condition");
        }
        StatType statType = statTypeRepository.findById(request.getStatTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Stat type not found"));
        if (modifierRepository.existsByConditionIdAndStatTypeId(conditionId, request.getStatTypeId())) {
            throw new DuplicateResourceException("Modifier for this stat type already exists on this condition");
        }
        ConditionModifier modifier = ConditionModifier.builder()
                .condition(condition)
                .statType(statType)
                .modifierValue(request.getModifierValue())
                .build();
        modifierRepository.save(modifier);
        condition.getModifiers().add(modifier);
        return toResponse(condition);
    }

    @Transactional
    public void deleteModifier(UUID conditionId, UUID modifierId, String username) {
        User gm = getGM(username);
        Condition condition = conditionRepository.findById(conditionId)
                .orElseThrow(() -> new ResourceNotFoundException("Condition not found"));
        if (!condition.getCreatedBy().getId().equals(gm.getId())) {
            throw new AccessDeniedException("You did not create this condition");
        }
        ConditionModifier modifier = modifierRepository.findById(modifierId)
                .orElseThrow(() -> new ResourceNotFoundException("Modifier not found"));
        if (!modifier.getCondition().getId().equals(conditionId)) {
            throw new ResourceNotFoundException("Modifier does not belong to this condition");
        }
        condition.getModifiers().remove(modifier);
        modifierRepository.delete(modifier);
    }

    @Transactional
    public CharacterConditionResponse applyCondition(UUID characterId, ApplyConditionRequest request, String username) {
        User gm = getGM(username);
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        if (!characterRepository.isPlayerInGameMasterTeam(character.getOwner().getId(), gm.getId())) {
            throw new AccessDeniedException("This character's owner is not in any of your teams");
        }
        Condition condition = conditionRepository.findById(request.getConditionId())
                .orElseThrow(() -> new ResourceNotFoundException("Condition not found"));
        if (charCondRepository.existsByCharacterIdAndConditionIdAndActiveTrue(characterId, condition.getId())) {
            throw new DuplicateResourceException("This condition is already active on this character");
        }
        CharacterCondition cc = CharacterCondition.builder()
                .character(character)
                .condition(condition)
                .appliedBy(gm)
                .active(true)
                .build();
        cc = charCondRepository.save(cc);
        return toCharCondResponse(cc);
    }

    @Transactional(readOnly = true)
    public List<CharacterConditionResponse> getActiveConditions(UUID characterId, String username) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        List<CharacterCondition> conditions = charCondRepository.findAllByCharacterIdAndActiveTrue(characterId);
        return conditions.stream().map(this::toCharCondResponse).toList();
    }

    @Transactional
    public void removeCondition(UUID characterId, UUID charConditionId, String username) {
        User gm = getGM(username);
        CharacterCondition cc = charCondRepository.findById(charConditionId)
                .orElseThrow(() -> new ResourceNotFoundException("Character condition not found"));
        if (!cc.getCharacter().getId().equals(characterId)) {
            throw new ResourceNotFoundException("Condition does not belong to this character");
        }
        if (!characterRepository.isPlayerInGameMasterTeam(cc.getCharacter().getOwner().getId(), gm.getId())) {
            throw new AccessDeniedException("This character's owner is not in any of your teams");
        }
        cc.setActive(false);
        charCondRepository.save(cc);
    }

    private User getGM(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getRole() != Role.GAME_MASTER) {
            throw new AccessDeniedException("Only game masters can manage conditions");
        }
        return user;
    }

    private ConditionResponse toResponse(Condition c) {
        List<ConditionModifierResponse> mods = c.getModifiers().stream()
                .map(m -> ConditionModifierResponse.builder()
                        .id(m.getId())
                        .statTypeId(m.getStatType().getId())
                        .statTypeName(m.getStatType().getName())
                        .modifierValue(m.getModifierValue())
                        .build())
                .toList();
        return ConditionResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .modifiers(mods)
                .createdById(c.getCreatedBy().getId())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private CharacterConditionResponse toCharCondResponse(CharacterCondition cc) {
        Condition c = cc.getCondition();
        List<ConditionModifierResponse> mods = c.getModifiers().stream()
                .map(m -> ConditionModifierResponse.builder()
                        .id(m.getId())
                        .statTypeId(m.getStatType().getId())
                        .statTypeName(m.getStatType().getName())
                        .modifierValue(m.getModifierValue())
                        .build())
                .toList();
        return CharacterConditionResponse.builder()
                .id(cc.getId())
                .conditionId(c.getId())
                .conditionName(c.getName())
                .conditionDescription(c.getDescription())
                .modifiers(mods)
                .appliedById(cc.getAppliedBy().getId())
                .appliedAt(cc.getAppliedAt())
                .active(cc.getActive())
                .build();
    }
}
