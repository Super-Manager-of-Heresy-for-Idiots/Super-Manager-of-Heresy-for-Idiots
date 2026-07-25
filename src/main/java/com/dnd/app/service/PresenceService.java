package com.dnd.app.service;

import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.CampaignLocation;
import com.dnd.app.domain.CampaignNpc;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.BattleStatus;
import com.dnd.app.domain.enums.MediaOwnerType;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.response.LocationOccupantsResponse;
import com.dnd.app.dto.response.LocationRefResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.BattleCombatantRepository;
import com.dnd.app.repository.CampaignLocationRepository;
import com.dnd.app.repository.CampaignNpcRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.service.media.MediaUrlResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Класс PresenceService описывает сервис бизнес-логики "присутствия в мире" (WORLD_PLAN Этап 1):
 * где находится персонаж (characters.current_location_id), где размещён NPC
 * (campaign_npcs.location_id) и кто сейчас находится в конкретной локации.
 * Правило co-presence ({@link #assertSameLocation}) переиспользуется квестами и торговлей.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PresenceService {

    private final CampaignLocationRepository locationRepository;
    private final PlayerCharacterRepository characterRepository;
    private final CampaignNpcRepository npcRepository;
    private final BattleCombatantRepository battleCombatantRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final WebSocketEventService webSocketEventService;
    private final MediaUrlResolver mediaUrlResolver;

    /**
     * Помещает персонажа в локацию ("войти"). Владелец персонажа входит сам (только в
     * видимые игрокам локации), ГМ может перемещать любого персонажа в любую локацию.
     * Вход запрещён, пока персонаж участвует в активном бою.
     * @param campaignId идентификатор кампании
     * @param locationId идентификатор целевой локации
     * @param characterId идентификатор персонажа
     * @param username имя пользователя, выполняющего действие
     * @return ссылка на локацию, в которой персонаж теперь находится
     */
    @Transactional
    public LocationRefResponse enterLocation(UUID campaignId, UUID locationId, UUID characterId, String username) {
        User user = getUser(username);
        CampaignLocation location = findLocationInCampaign(locationId, campaignId);
        campaignService.enforceMembershipOrAdmin(location.getCampaign(), user);

        boolean gm = isGmOrAdmin(campaignId, user);
        if (!gm && !Boolean.TRUE.equals(location.getIsVisibleToPlayers())) {
            throw new ResourceNotFoundException("Location not found");
        }

        PlayerCharacter character = resolveCampaignCharacter(characterId, campaignId, user, gm);
        assertNotInActiveBattle(character);

        character.setCurrentLocation(location);
        characterRepository.save(character);

        log.info("Character entered location: characterId={}, locationId={}, by={}", characterId, locationId, username);
        sendPresenceEvent(campaignId, characterId, "CHARACTER", character.getName(), locationId, "ENTER", user);
        return toRef(location);
    }

    /**
     * Убирает персонажа из его текущей локации ("выйти в никуда").
     * @param campaignId идентификатор кампании
     * @param locationId идентификатор локации, из которой уходит персонаж
     * @param characterId идентификатор персонажа
     * @param username имя пользователя, выполняющего действие
     */
    @Transactional
    public void leaveLocation(UUID campaignId, UUID locationId, UUID characterId, String username) {
        User user = getUser(username);
        CampaignLocation location = findLocationInCampaign(locationId, campaignId);
        campaignService.enforceMembershipOrAdmin(location.getCampaign(), user);

        boolean gm = isGmOrAdmin(campaignId, user);
        PlayerCharacter character = resolveCampaignCharacter(characterId, campaignId, user, gm);
        if (character.getCurrentLocation() == null || !character.getCurrentLocation().getId().equals(locationId)) {
            throw new BadRequestException("The character is not in that location");
        }
        assertNotInActiveBattle(character);

        character.setCurrentLocation(null);
        characterRepository.save(character);

        log.info("Character left location: characterId={}, locationId={}, by={}", characterId, locationId, username);
        sendPresenceEvent(campaignId, characterId, "CHARACTER", character.getName(), locationId, "LEAVE", user);
    }

    /**
     * Возвращает обитателей локации: персонажей игроков и размещённых NPC.
     * Для не-ГМ скрытые NPC не показываются, а невидимая локация недоступна.
     * @param campaignId идентификатор кампании
     * @param locationId идентификатор локации
     * @param username имя пользователя, выполняющего запрос
     * @return состав обитателей локации
     */
    @Transactional(readOnly = true)
    public LocationOccupantsResponse getOccupants(UUID campaignId, UUID locationId, String username) {
        User user = getUser(username);
        CampaignLocation location = findLocationInCampaign(locationId, campaignId);
        campaignService.enforceMembershipOrAdmin(location.getCampaign(), user);

        boolean gm = isGmOrAdmin(campaignId, user);
        if (!gm && !Boolean.TRUE.equals(location.getIsVisibleToPlayers())) {
            throw new ResourceNotFoundException("Location not found");
        }

        List<PlayerCharacter> characters = characterRepository.findByCurrentLocationId(locationId);
        List<CampaignNpc> npcs = gm
                ? npcRepository.findByLocationId(locationId)
                : npcRepository.findByLocationIdAndIsVisibleToPlayersTrue(locationId);

        return LocationOccupantsResponse.builder()
                .locationId(locationId)
                .characters(characters.stream().map(this::toCharacterOccupant).toList())
                .npcs(npcs.stream().map(this::toNpcOccupant).toList())
                .build();
    }

    /**
     * Размещает NPC в локации кампании или снимает его с карты мира (locationId = null).
     * Доступно только ГМ кампании (или админу).
     * @param campaignId идентификатор кампании
     * @param npcId идентификатор NPC
     * @param locationId целевая локация или null
     * @param username имя пользователя, выполняющего действие
     * @return ссылка на локацию NPC (null, если NPC снят с локации)
     */
    @Transactional
    public LocationRefResponse setNpcLocation(UUID campaignId, UUID npcId, UUID locationId, String username) {
        User user = getUser(username);
        CampaignNpc npc = npcRepository.findById(npcId)
                .orElseThrow(() -> new ResourceNotFoundException("NPC not found"));
        if (npc.getCampaign() == null || !npc.getCampaign().getId().equals(campaignId)) {
            throw new ResourceNotFoundException("NPC not found in this campaign");
        }
        campaignService.enforceGmOrAdmin(npc.getCampaign(), user);

        UUID previousLocationId = npc.getLocation() != null ? npc.getLocation().getId() : null;
        CampaignLocation location = null;
        if (locationId != null) {
            location = findLocationInCampaign(locationId, campaignId);
        }
        npc.setLocation(location);
        npcRepository.save(npc);

        log.info("NPC location set: npcId={}, locationId={}, by={}", npcId, locationId, username);
        if (previousLocationId != null && !previousLocationId.equals(locationId)) {
            sendPresenceEvent(campaignId, npcId, "NPC", npc.getName(), previousLocationId, "LEAVE", user);
        }
        if (locationId != null && !locationId.equals(previousLocationId)) {
            sendPresenceEvent(campaignId, npcId, "NPC", npc.getName(), locationId, "ENTER", user);
        }
        return location != null ? toRef(location) : null;
    }

    /**
     * Правило co-presence: персонаж и NPC должны находиться в одной локации, иначе
     * игровое взаимодействие (осмотр, квест, торговля) невозможно. ГМ-флоу может
     * пропускать эту проверку на своей стороне.
     * @param character персонаж игрока
     * @param npc NPC, с которым взаимодействуют
     */
    public void assertSameLocation(PlayerCharacter character, CampaignNpc npc) {
        CampaignLocation charLoc = character.getCurrentLocation();
        CampaignLocation npcLoc = npc.getLocation();
        if (charLoc == null || npcLoc == null || !charLoc.getId().equals(npcLoc.getId())) {
            throw new BadRequestException("The character must be in the same location as this NPC");
        }
    }

    // --- Private helpers ---

    private void assertNotInActiveBattle(PlayerCharacter character) {
        boolean inBattle = !battleCombatantRepository
                .findByCharacter_IdAndBattle_Status(character.getId(), BattleStatus.ACTIVE)
                .isEmpty();
        if (inBattle) {
            throw new BadRequestException("Cannot change location while the character is in an active battle");
        }
    }

    private void sendPresenceEvent(UUID campaignId, UUID entityId, String entityType, String entityName,
                                   UUID locationId, String direction, User user) {
        webSocketEventService.sendCampaignEvent(
                WebSocketEventType.LOCATION_PRESENCE_CHANGED,
                campaignId,
                Map.of(
                        "locationId", locationId,
                        "entityType", entityType,
                        "entityId", entityId,
                        "entityName", entityName,
                        "direction", direction),
                user.getId());
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

    private CampaignLocation findLocationInCampaign(UUID locationId, UUID campaignId) {
        CampaignLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found"));
        Campaign campaign = location.getCampaign();
        if (campaign == null || !campaign.getId().equals(campaignId)) {
            throw new ResourceNotFoundException("Location not found in this campaign");
        }
        return location;
    }

    private boolean isGmOrAdmin(UUID campaignId, User user) {
        return user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId());
    }

    private LocationRefResponse toRef(CampaignLocation location) {
        return LocationRefResponse.builder().id(location.getId()).name(location.getName()).build();
    }

    private LocationOccupantsResponse.CharacterOccupant toCharacterOccupant(PlayerCharacter character) {
        return LocationOccupantsResponse.CharacterOccupant.builder()
                .id(character.getId())
                .name(character.getName())
                .ownerUserId(character.getOwner() != null ? character.getOwner().getId() : null)
                .ownerUsername(character.getOwner() != null ? character.getOwner().getUsername() : null)
                .avatarUrl(character.getAvatarUrl())
                .build();
    }

    private LocationOccupantsResponse.NpcOccupant toNpcOccupant(CampaignNpc npc) {
        return LocationOccupantsResponse.NpcOccupant.builder()
                .id(npc.getId())
                .name(npc.getName())
                .npcRole(npc.getNpcRole())
                .portraitUrl(mediaUrlResolver.resolve(MediaOwnerType.NPC_PORTRAIT, npc.getId(), null))
                .isVisibleToPlayers(npc.getIsVisibleToPlayers())
                .build();
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
