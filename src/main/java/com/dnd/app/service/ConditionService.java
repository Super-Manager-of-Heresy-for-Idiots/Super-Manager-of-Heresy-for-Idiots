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
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConditionService {

    private final ConditionRepository conditionRepository;
    private final ConditionModifierRepository modifierRepository;
    private final CharacterConditionRepository charCondRepository;
    private final StatTypeRepository statTypeRepository;
    private final PlayerCharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    @Transactional
    public ConditionResponse createCondition(CreateConditionRequest request, String username) {
        User gm = getGMOrAdmin(username);

        Team team = null;
        if (gm.getRole() == Role.GAME_MASTER) {
            if (request.getTeamId() == null) {
                throw new BadRequestException("Мастер игры должен указать команду для состояния");
            }
            team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Команда не найдена"));
            if (!team.getGameMaster().getId().equals(gm.getId())) {
                throw new AccessDeniedException("Вы не являетесь мастером этой команды");
            }
        } else if (gm.getRole() == Role.ADMIN && request.getTeamId() != null) {
            team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Команда не найдена"));
        }

        if (conditionRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Состояние с таким названием уже существует");
        }
        Condition condition = Condition.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(gm)
                .team(team)
                .build();
        condition = conditionRepository.save(condition);
        log.info("Condition created: id={}, name='{}', teamId={}, by gm={}", condition.getId(), condition.getName(), team != null ? team.getId() : "global", username);
        return toResponse(condition);
    }

    @Transactional(readOnly = true)
    public List<ConditionResponse> listConditions(String username, UUID teamId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        List<Condition> conditions;
        if (user.getRole() == Role.ADMIN) {
            if (teamId != null) {
                conditions = conditionRepository.findAllByTeamId(teamId);
            } else {
                conditions = conditionRepository.findAll();
            }
        } else if (user.getRole() == Role.GAME_MASTER) {
            if (teamId != null) {
                Team team = teamRepository.findById(teamId)
                        .orElseThrow(() -> new ResourceNotFoundException("Команда не найдена"));
                if (!team.getGameMaster().getId().equals(user.getId())) {
                    throw new AccessDeniedException("Вы не являетесь мастером этой команды");
                }
                conditions = conditionRepository.findAllByTeamId(teamId);
            } else {
                conditions = conditionRepository.findAllByGameMasterId(user.getId());
            }
        } else {
            throw new AccessDeniedException("Игроки не могут просматривать список состояний");
        }
        return conditions.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ConditionResponse getCondition(UUID id, String username) {
        Condition condition = conditionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Состояние не найдено"));
        return toResponse(condition);
    }

    @Transactional
    public ConditionResponse updateCondition(UUID id, CreateConditionRequest request, String username) {
        User gm = getGMOrAdmin(username);
        Condition condition = conditionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Состояние не найдено"));
        if (gm.getRole() == Role.GAME_MASTER) {
            if (condition.getTeam() == null || !condition.getTeam().getGameMaster().getId().equals(gm.getId())) {
                throw new AccessDeniedException("Это состояние не принадлежит вашей команде");
            }
        }
        if (!condition.getName().equals(request.getName()) && conditionRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Состояние с таким названием уже существует");
        }
        condition.setName(request.getName());
        condition.setDescription(request.getDescription());
        condition = conditionRepository.save(condition);
        return toResponse(condition);
    }

    @Transactional
    public void deleteCondition(UUID id, String username) {
        User gm = getGMOrAdmin(username);
        Condition condition = conditionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Состояние не найдено"));
        if (gm.getRole() == Role.GAME_MASTER) {
            if (condition.getTeam() == null || !condition.getTeam().getGameMaster().getId().equals(gm.getId())) {
                throw new AccessDeniedException("Это состояние не принадлежит вашей команде");
            }
        }
        log.info("Condition deleted: id={}, name='{}', by gm={}", id, condition.getName(), username);
        conditionRepository.delete(condition);
    }

    @Transactional
    public ConditionResponse addModifier(UUID conditionId, AddConditionModifierRequest request, String username) {
        User gm = getGMOrAdmin(username);
        Condition condition = conditionRepository.findById(conditionId)
                .orElseThrow(() -> new ResourceNotFoundException("Состояние не найдено"));
        if (gm.getRole() == Role.GAME_MASTER) {
            if (condition.getTeam() == null || !condition.getTeam().getGameMaster().getId().equals(gm.getId())) {
                throw new AccessDeniedException("Это состояние не принадлежит вашей команде");
            }
        }
        StatType statType = statTypeRepository.findById(request.getStatTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Характеристика не найдена"));
        if (modifierRepository.existsByConditionIdAndStatTypeId(conditionId, request.getStatTypeId())) {
            throw new DuplicateResourceException("Модификатор этой характеристики уже есть у состояния");
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
        User gm = getGMOrAdmin(username);
        Condition condition = conditionRepository.findById(conditionId)
                .orElseThrow(() -> new ResourceNotFoundException("Состояние не найдено"));
        if (gm.getRole() == Role.GAME_MASTER) {
            if (condition.getTeam() == null || !condition.getTeam().getGameMaster().getId().equals(gm.getId())) {
                throw new AccessDeniedException("Это состояние не принадлежит вашей команде");
            }
        }
        ConditionModifier modifier = modifierRepository.findById(modifierId)
                .orElseThrow(() -> new ResourceNotFoundException("Модификатор не найден"));
        if (!modifier.getCondition().getId().equals(conditionId)) {
            throw new ResourceNotFoundException("Модификатор не относится к этому состоянию");
        }
        condition.getModifiers().remove(modifier);
        modifierRepository.delete(modifier);
    }

    @Transactional
    public CharacterConditionResponse applyCondition(UUID characterId, ApplyConditionRequest request, String username) {
        User gm = getGMOrAdmin(username);
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
        if (gm.getRole() == Role.GAME_MASTER) {
            if (character.getTeam() == null || !character.getTeam().getGameMaster().getId().equals(gm.getId())) {
                throw new AccessDeniedException("Этот персонаж не принадлежит вашей команде");
            }
        }
        Condition condition = conditionRepository.findById(request.getConditionId())
                .orElseThrow(() -> new ResourceNotFoundException("Состояние не найдено"));
        if (gm.getRole() == Role.GAME_MASTER && condition.getTeam() != null
                && !condition.getTeam().getGameMaster().getId().equals(gm.getId())) {
            throw new AccessDeniedException("Это состояние не принадлежит вашей команде");
        }
        if (charCondRepository.existsByCharacterIdAndConditionIdAndActiveTrue(characterId, condition.getId())) {
            throw new DuplicateResourceException("Это состояние уже активно у персонажа");
        }
        CharacterCondition cc = CharacterCondition.builder()
                .character(character)
                .condition(condition)
                .appliedBy(gm)
                .active(true)
                .build();
        cc = charCondRepository.save(cc);
        log.info("Condition applied: condition='{}', characterId={}, by gm={}", condition.getName(), characterId, username);
        return toCharCondResponse(cc);
    }

    @Transactional(readOnly = true)
    public List<CharacterConditionResponse> getActiveConditions(UUID characterId, String username) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (user.getRole() == Role.PLAYER && !character.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Этот персонаж вам не принадлежит");
        }
        if (user.getRole() == Role.GAME_MASTER) {
            if (character.getTeam() == null || !character.getTeam().getGameMaster().getId().equals(user.getId())) {
                throw new AccessDeniedException("Этот персонаж не принадлежит вашей команде");
            }
        }
        List<CharacterCondition> conditions = charCondRepository.findAllByCharacterIdAndActiveTrue(characterId);
        return conditions.stream().map(this::toCharCondResponse).toList();
    }

    @Transactional
    public void removeCondition(UUID characterId, UUID charConditionId, String username) {
        User gm = getGMOrAdmin(username);
        CharacterCondition cc = charCondRepository.findById(charConditionId)
                .orElseThrow(() -> new ResourceNotFoundException("Состояние персонажа не найдено"));
        if (!cc.getCharacter().getId().equals(characterId)) {
            throw new ResourceNotFoundException("Состояние не относится к этому персонажу");
        }
        if (gm.getRole() == Role.GAME_MASTER) {
            if (cc.getCharacter().getTeam() == null || !cc.getCharacter().getTeam().getGameMaster().getId().equals(gm.getId())) {
                throw new AccessDeniedException("Этот персонаж не принадлежит вашей команде");
            }
        }
        cc.setActive(false);
        charCondRepository.save(cc);
        log.info("Condition removed: conditionId={}, characterId={}, by gm={}", charConditionId, characterId, username);
    }

    private User getGMOrAdmin(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (user.getRole() != Role.GAME_MASTER && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Только мастера игры могут управлять состояниями");
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
                .teamId(c.getTeam() != null ? c.getTeam().getId() : null)
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
