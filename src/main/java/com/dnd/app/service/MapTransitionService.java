package com.dnd.app.service;

import com.dnd.app.domain.Battle;
import com.dnd.app.domain.BattleCombatant;
import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.CampaignLocation;
import com.dnd.app.domain.MapTransition;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.BattleStatus;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.MapCellDto;
import com.dnd.app.dto.request.CreateMapTransitionRequest;
import com.dnd.app.dto.request.TraverseTransitionRequest;
import com.dnd.app.dto.request.UpdateMapTransitionRequest;
import com.dnd.app.dto.response.LocationRefResponse;
import com.dnd.app.dto.response.MapTransitionResponse;
import com.dnd.app.dto.response.TraverseResultResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.integration.map.MapTokenReader;
import com.dnd.app.integration.map.MapTokenRelocator;
import com.dnd.app.repository.BattleCombatantRepository;
import com.dnd.app.repository.BattleRepository;
import com.dnd.app.repository.CampaignLocationRepository;
import com.dnd.app.repository.MapTransitionRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Класс MapTransitionService описывает переходы между картами через "ключевые клетки"
 * (WORLD_PLAN Этап 5): CRUD для ГМ, чтение для отрисовки слоя переходов и игровой
 * сценарий traverse — проход персонажа через переход со сменой локации мира,
 * переносом токена (best-effort через map-service) и корректным выходом из боя.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MapTransitionService {

    private final MapTransitionRepository transitionRepository;
    private final CampaignLocationRepository locationRepository;
    private final PlayerCharacterRepository characterRepository;
    private final BattleCombatantRepository battleCombatantRepository;
    private final BattleRepository battleRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final WebSocketEventService webSocketEventService;
    private final MapTokenReader mapTokenReader;
    private final MapTokenRelocator mapTokenRelocator;
    private final ObjectMapper objectMapper;

    /**
     * Возвращает переходы кампании; при mapId — только исходящие с этой карты.
     * Игроки видят только включённые переходы.
     * @param campaignId идентификатор кампании
     * @param mapId фильтр по исходной карте (опционально)
     * @param username имя пользователя, выполняющего запрос
     * @return список переходов
     */
    @Transactional(readOnly = true)
    public List<MapTransitionResponse> listTransitions(UUID campaignId, UUID mapId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        boolean gm = isGmOrAdmin(campaignId, user);

        List<MapTransition> transitions = mapId != null
                ? transitionRepository.findByCampaignIdAndFromMapId(campaignId, mapId)
                : transitionRepository.findByCampaignId(campaignId);
        return transitions.stream()
                .filter(t -> gm || Boolean.TRUE.equals(t.getEnabled()))
                .map(this::toResponse)
                .toList();
    }

    /**
     * Создаёт переход между картами (GM only).
     * @param campaignId идентификатор кампании
     * @param request входящие данные запроса
     * @param username имя пользователя, выполняющего действие
     * @return созданный переход
     */
    @Transactional
    public MapTransitionResponse createTransition(UUID campaignId, CreateMapTransitionRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);

        CampaignLocation toLocation = resolveLocation(request.getToLocationId(), campaignId);

        MapTransition transition = MapTransition.builder()
                .campaign(campaign)
                .fromMapId(request.getFromMapId())
                .fromCellsJson(writeJson(request.getFromCells()))
                .toMapId(request.getToMapId())
                .toCellJson(writeJson(request.getToCell()))
                .toLocation(toLocation)
                .label(request.getLabel())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .createdBy(user)
                .build();
        transition = transitionRepository.save(transition);

        log.info("Map transition created: id={}, fromMap={}, toMap={}, by={}",
                transition.getId(), request.getFromMapId(), request.getToMapId(), username);
        return toResponse(transition);
    }

    /**
     * Обновляет переход (GM only). null-поля не меняются.
     * @param campaignId идентификатор кампании
     * @param transitionId идентификатор перехода
     * @param request входящие данные запроса
     * @param username имя пользователя, выполняющего действие
     * @return обновлённый переход
     */
    @Transactional
    public MapTransitionResponse updateTransition(UUID campaignId, UUID transitionId,
                                                  UpdateMapTransitionRequest request, String username) {
        User user = getUser(username);
        MapTransition transition = findTransition(transitionId, campaignId);
        campaignService.enforceGmOrAdmin(transition.getCampaign(), user);

        if (request.getFromCells() != null && !request.getFromCells().isEmpty()) {
            transition.setFromCellsJson(writeJson(request.getFromCells()));
        }
        if (request.getToCell() != null) {
            transition.setToCellJson(writeJson(request.getToCell()));
        }
        if (Boolean.TRUE.equals(request.getClearToLocation())) {
            transition.setToLocation(null);
        } else if (request.getToLocationId() != null) {
            transition.setToLocation(resolveLocation(request.getToLocationId(), campaignId));
        }
        if (request.getLabel() != null) {
            transition.setLabel(request.getLabel());
        }
        if (request.getEnabled() != null) {
            transition.setEnabled(request.getEnabled());
        }
        transition = transitionRepository.save(transition);

        log.info("Map transition updated: id={}, by={}", transitionId, username);
        return toResponse(transition);
    }

    /**
     * Удаляет переход (GM only).
     * @param campaignId идентификатор кампании
     * @param transitionId идентификатор перехода
     * @param username имя пользователя, выполняющего действие
     */
    @Transactional
    public void deleteTransition(UUID campaignId, UUID transitionId, String username) {
        User user = getUser(username);
        MapTransition transition = findTransition(transitionId, campaignId);
        campaignService.enforceGmOrAdmin(transition.getCampaign(), user);
        transitionRepository.delete(transition);
        log.info("Map transition deleted: id={}, by={}", transitionId, username);
    }

    /**
     * Проход персонажа через переход. Правила:
     * игрок водит только своего персонажа; переход должен быть включён (ГМ — всегда);
     * при известной позиции токена (map-service доступен) токен обязан стоять на одной
     * из ключевых клеток; в активном бою переход разрешён только в свой ход и означает
     * выход из боя (с корректировкой указателя хода). Затем: смена локации мира,
     * best-effort перенос токена на целевую карту и WS-события.
     * @param campaignId идентификатор кампании
     * @param transitionId идентификатор перехода
     * @param request входящие данные запроса
     * @param username имя пользователя, выполняющего действие
     * @return результат прохода
     */
    @Transactional
    public TraverseResultResponse traverse(UUID campaignId, UUID transitionId,
                                           TraverseTransitionRequest request, String username) {
        User user = getUser(username);
        MapTransition transition = findTransition(transitionId, campaignId);
        campaignService.enforceMembershipOrAdmin(transition.getCampaign(), user);
        boolean gm = isGmOrAdmin(campaignId, user);

        if (!gm && !Boolean.TRUE.equals(transition.getEnabled())) {
            throw new BadRequestException("This passage is locked");
        }

        PlayerCharacter character = resolveCampaignCharacter(request.getCharacterId(), campaignId, user, gm);

        // Серверная валидация позиции: токен должен стоять на одной из ключевых клеток.
        // Позиция известна только при живой интеграции с map-service; иначе доверяем факту
        // подтверждённого TOKEN_MOVED на фронте (доска — ответственность map-service).
        List<MapCellDto> fromCells = readCells(transition.getFromCellsJson());
        if (request.getTokenId() != null && request.getFromSessionId() != null) {
            mapTokenReader.getTokenPosition(request.getFromSessionId(), request.getTokenId())
                    .ifPresent(pos -> {
                        boolean onCell = fromCells.stream().anyMatch(c ->
                                c.getGridX() != null && c.getGridY() != null
                                        && c.getGridX() == pos.gridX() && c.getGridY() == pos.gridY());
                        if (!onCell) {
                            throw new BadRequestException("The token is not standing on the transition cell");
                        }
                        if (pos.characterId() != null && !pos.characterId().equals(character.getId())) {
                            throw new BadRequestException("The token does not belong to that character");
                        }
                    });
        }

        // Активный бой: переход разрешён только в свой ход (или ГМом) и означает выход из боя.
        boolean leftBattle = leaveActiveBattleIfAny(character, gm, user, campaignId, transition);

        // Смена локации мира.
        UUID previousLocationId = character.getCurrentLocation() != null
                ? character.getCurrentLocation().getId() : null;
        CampaignLocation toLocation = transition.getToLocation();
        if (toLocation != null) {
            character.setCurrentLocation(toLocation);
            characterRepository.save(character);
            sendPresenceEvents(campaignId, character, previousLocationId, toLocation.getId(), user);
        }

        // Best-effort перенос токена на целевую карту (map-service может быть недоступен).
        MapCellDto toCell = readCell(transition.getToCellJson());
        boolean tokenMoved = false;
        if (request.getTokenId() != null && request.getFromSessionId() != null) {
            tokenMoved = mapTokenRelocator.relocate(new MapTokenRelocator.RelocationSpec(
                    request.getFromSessionId(), request.getTokenId(), transition.getToMapId(),
                    toCell.getGridX() != null ? toCell.getGridX() : 0,
                    toCell.getGridY() != null ? toCell.getGridY() : 0));
        }

        log.info("Map transition traversed: transitionId={}, characterId={}, toMap={}, tokenMoved={}, leftBattle={}, by={}",
                transitionId, character.getId(), transition.getToMapId(), tokenMoved, leftBattle, username);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("transitionId", transitionId);
        eventData.put("characterId", character.getId());
        eventData.put("fromMapId", transition.getFromMapId());
        eventData.put("toMapId", transition.getToMapId());
        if (toLocation != null) {
            eventData.put("toLocationId", toLocation.getId());
        }
        eventData.put("tokenMoved", tokenMoved);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.MAP_TRANSITION_TRAVERSED,
                campaignId, character.getId(), eventData, user.getId());

        return TraverseResultResponse.builder()
                .transitionId(transitionId)
                .characterId(character.getId())
                .toMapId(transition.getToMapId())
                .toCell(toCell)
                .toLocation(toLocation == null ? null : LocationRefResponse.builder()
                        .id(toLocation.getId()).name(toLocation.getName()).build())
                .tokenMoved(tokenMoved)
                .leftBattle(leftBattle)
                .build();
    }

    // --- Private helpers ---

    /**
     * Если персонаж — участник активного боя: игроку разрешаем переход только в свой ход,
     * ГМу — всегда; участник удаляется из боя, указатель текущего хода корректируется
     * (семантика как у end-turn при выпадении участника).
     */
    private boolean leaveActiveBattleIfAny(PlayerCharacter character, boolean gm, User user,
                                           UUID campaignId, MapTransition transition) {
        List<BattleCombatant> active = battleCombatantRepository
                .findByCharacter_IdAndBattle_Status(character.getId(), BattleStatus.ACTIVE);
        if (active.isEmpty()) {
            return false;
        }
        BattleCombatant combatant = active.get(0);
        Battle battle = combatant.getBattle();
        List<BattleCombatant> ordered = battleCombatantRepository
                .findByBattleIdOrderByTurnOrderAsc(battle.getId());

        int idx = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getId().equals(combatant.getId())) {
                idx = i;
                break;
            }
        }
        int current = battle.getCurrentTurnIndex() != null ? battle.getCurrentTurnIndex() : 0;
        if (!ordered.isEmpty()) {
            current = Math.floorMod(current, ordered.size());
        }
        if (!gm && idx != current) {
            throw new BadRequestException("You can only use a transition on your turn while in battle");
        }

        battleCombatantRepository.delete(combatant);

        int newSize = ordered.size() - 1;
        if (newSize <= 0) {
            battle.setCurrentTurnIndex(0);
        } else if (idx < current) {
            battle.setCurrentTurnIndex(current - 1);
        } else if (idx == current && current >= newSize) {
            // Ушедший ходил последним в раунде — ход переходит к началу следующего раунда.
            battle.setCurrentTurnIndex(0);
            battle.setRoundNumber((battle.getRoundNumber() != null ? battle.getRoundNumber() : 1) + 1);
        }
        battleRepository.save(battle);

        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_UPDATED, campaignId,
                Map.of("battleId", battle.getId(), "reason", "COMBATANT_LEFT_VIA_TRANSITION",
                        "combatantId", combatant.getId(),
                        "transitionId", transition.getId()), user.getId());
        return true;
    }

    private void sendPresenceEvents(UUID campaignId, PlayerCharacter character,
                                    UUID fromLocationId, UUID toLocationId, User user) {
        if (fromLocationId != null && !fromLocationId.equals(toLocationId)) {
            webSocketEventService.sendCampaignEvent(WebSocketEventType.LOCATION_PRESENCE_CHANGED, campaignId,
                    Map.of("locationId", fromLocationId, "entityType", "CHARACTER",
                            "entityId", character.getId(), "entityName", character.getName(),
                            "direction", "LEAVE"), user.getId());
        }
        if (toLocationId != null && !toLocationId.equals(fromLocationId)) {
            webSocketEventService.sendCampaignEvent(WebSocketEventType.LOCATION_PRESENCE_CHANGED, campaignId,
                    Map.of("locationId", toLocationId, "entityType", "CHARACTER",
                            "entityId", character.getId(), "entityName", character.getName(),
                            "direction", "ENTER"), user.getId());
        }
    }

    private CampaignLocation resolveLocation(UUID locationId, UUID campaignId) {
        if (locationId == null) {
            return null;
        }
        CampaignLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found"));
        if (location.getCampaign() == null || !location.getCampaign().getId().equals(campaignId)) {
            throw new BadRequestException("Location does not belong to this campaign");
        }
        return location;
    }

    private PlayerCharacter resolveCampaignCharacter(UUID characterId, UUID campaignId, User user, boolean gm) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        if (character.getCampaign() == null || !character.getCampaign().getId().equals(campaignId)) {
            throw new BadRequestException("Character does not belong to this campaign");
        }
        if (!gm && (character.getOwner() == null || !character.getOwner().getId().equals(user.getId()))) {
            throw new AccessDeniedException("You can only move your own characters");
        }
        return character;
    }

    private MapTransition findTransition(UUID transitionId, UUID campaignId) {
        MapTransition transition = transitionRepository.findById(transitionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transition not found"));
        if (transition.getCampaign() == null || !transition.getCampaign().getId().equals(campaignId)) {
            throw new ResourceNotFoundException("Transition not found in this campaign");
        }
        return transition;
    }

    private boolean isGmOrAdmin(UUID campaignId, User user) {
        return user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BadRequestException("Invalid cell payload");
        }
    }

    private List<MapCellDto> readCells(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<MapCellDto>>() { });
        } catch (Exception e) {
            log.warn("Corrupted from_cells_json: {}", e.getMessage());
            return List.of();
        }
    }

    private MapCellDto readCell(String json) {
        try {
            return objectMapper.readValue(json, MapCellDto.class);
        } catch (Exception e) {
            log.warn("Corrupted to_cell_json: {}", e.getMessage());
            return MapCellDto.builder().gridX(0).gridY(0).build();
        }
    }

    private MapTransitionResponse toResponse(MapTransition transition) {
        return MapTransitionResponse.builder()
                .id(transition.getId())
                .campaignId(transition.getCampaign().getId())
                .fromMapId(transition.getFromMapId())
                .fromCells(readCells(transition.getFromCellsJson()))
                .toMapId(transition.getToMapId())
                .toCell(readCell(transition.getToCellJson()))
                .toLocation(transition.getToLocation() == null ? null : LocationRefResponse.builder()
                        .id(transition.getToLocation().getId())
                        .name(transition.getToLocation().getName())
                        .build())
                .label(transition.getLabel())
                .enabled(transition.getEnabled())
                .createdAt(transition.getCreatedAt())
                .updatedAt(transition.getUpdatedAt())
                .build();
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
