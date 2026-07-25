package com.dnd.app.service;

import com.dnd.app.domain.CampaignNpc;
import com.dnd.app.domain.NpcShopItem;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.MediaOwnerType;
import com.dnd.app.domain.enums.NpcRole;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.response.CharacterQuestResponse;
import com.dnd.app.dto.response.LocationRefResponse;
import com.dnd.app.dto.response.NpcInteractionResponse;
import com.dnd.app.dto.response.QuestResponse;
import com.dnd.app.dto.response.ShopItemResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CampaignNpcRepository;
import com.dnd.app.repository.CharacterWalletRepository;
import com.dnd.app.repository.CurrencyTypeRepository;
import com.dnd.app.repository.NpcShopItemRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.service.media.MediaUrlResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Класс NpcInteractionService описывает игровой сценарий "подойти к NPC" (WORLD_PLAN Этап 3):
 * один агрегированный ответ с публичным описанием, доступными квестами и витриной торговца.
 * Гейт — co-presence персонажа и NPC в одной локации ({@link PresenceService}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NpcInteractionService {

    private static final String GOLD_SLUG = "gp";

    private final CampaignNpcRepository npcRepository;
    private final PlayerCharacterRepository characterRepository;
    private final NpcShopItemRepository shopItemRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final PresenceService presenceService;
    private final CharacterQuestService characterQuestService;
    private final MediaUrlResolver mediaUrlResolver;
    private final CharacterWalletRepository characterWalletRepository;
    private final CurrencyTypeRepository currencyTypeRepository;
    private final NpcDialogueService npcDialogueService;
    private final TradeService tradeService;

    /**
     * Возвращает агрегированное взаимодействие с NPC для персонажа: осмотр + квесты + витрина.
     * Игрок обязан находиться в одной локации с видимым NPC; ГМ смотрит без ограничений.
     * @param campaignId идентификатор кампании
     * @param npcId идентификатор NPC
     * @param characterId идентификатор персонажа игрока (обязателен для не-ГМ)
     * @param username имя пользователя, выполняющего запрос
     * @return агрегированный ответ взаимодействия
     */
    @Transactional(readOnly = true)
    public NpcInteractionResponse interact(UUID campaignId, UUID npcId, UUID characterId, String username) {
        User user = getUser(username);
        CampaignNpc npc = findNpcInCampaign(npcId, campaignId);
        campaignService.enforceMembershipOrAdmin(npc.getCampaign(), user);
        boolean gm = isGmOrAdmin(campaignId, user);

        if (!gm) {
            if (characterId == null) {
                throw new BadRequestException("characterId is required to interact with an NPC");
            }
            if (!Boolean.TRUE.equals(npc.getIsVisibleToPlayers())) {
                throw new ResourceNotFoundException("NPC not found");
            }
            PlayerCharacter character = resolveOwnCharacter(characterId, campaignId, user);
            presenceService.assertSameLocation(character, npc);
        }

        List<QuestResponse> quests = characterQuestService.listAvailableQuests(campaignId, npcId, characterId, username);
        List<CharacterQuestResponse> turnInQuests = characterId == null ? null
                : characterQuestService.listTurnInQuests(campaignId, npcId, characterId, username);

        List<ShopItemResponse> shopItems = null;
        if (npc.getNpcRole() == NpcRole.MERCHANT) {
            shopItems = shopItemRepository.findByNpcId(npcId).stream().map(this::toShopItem).toList();
        }

        return NpcInteractionResponse.builder()
                .npcId(npc.getId())
                .name(npc.getName())
                .publicDescription(npc.getPublicDescription())
                .npcRole(npc.getNpcRole())
                .portraitUrl(mediaUrlResolver.resolve(MediaOwnerType.NPC_PORTRAIT, npc.getId(), null))
                .location(npc.getLocation() == null ? null
                        : LocationRefResponse.builder()
                                .id(npc.getLocation().getId())
                                .name(npc.getLocation().getName())
                                .build())
                .availableQuests(quests)
                .turnInQuests(turnInQuests)
                .shopItems(shopItems)
                .buybackRatePercent(npc.getNpcRole() == NpcRole.MERCHANT
                        ? TradeService.BUYBACK_RATE_PERCENT : null)
                .dialogue(npcDialogueService.resolveForInteraction(npcId))
                .goldBalance(characterId == null ? null : resolveGoldBalance(characterId))
                .build();
    }

    // --- Private helpers ---

    /**
     * Возвращает баланс золота персонажа для отображения в окне торговли, либо null,
     * если валюта золота не настроена или кошелёк ещё не создан.
     */
    private BigDecimal resolveGoldBalance(UUID characterId) {
        return currencyTypeRepository.findBySlugAndHomebrewIsNull(GOLD_SLUG)
                .flatMap(gold -> characterWalletRepository
                        .findByCharacterIdAndCurrencyTypeId(characterId, gold.getId()))
                .map(wallet -> wallet.getAmount())
                .orElse(null);
    }

    private ShopItemResponse toShopItem(NpcShopItem line) {
        // Цена считается общим методом TradeService, чтобы игрок видел ровно то, что заплатит
        // (включая опциональный модификатор цен торговца, WORLD_PLAN Этап 5).
        return ShopItemResponse.builder()
                .id(line.getId())
                .itemTemplateId(line.getItemTemplate().getId())
                .itemName(line.getItemTemplate().getName())
                .priceGold(tradeService.resolveUnitPrice(line))
                .quantity(line.getQuantity())
                .restockQuantity(line.getRestockQuantity())
                .build();
    }

    private CampaignNpc findNpcInCampaign(UUID npcId, UUID campaignId) {
        CampaignNpc npc = npcRepository.findById(npcId)
                .orElseThrow(() -> new ResourceNotFoundException("NPC not found"));
        if (npc.getCampaign() == null || !npc.getCampaign().getId().equals(campaignId)) {
            throw new ResourceNotFoundException("NPC not found in this campaign");
        }
        return npc;
    }

    private PlayerCharacter resolveOwnCharacter(UUID characterId, UUID campaignId, User user) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        if (character.getCampaign() == null || !character.getCampaign().getId().equals(campaignId)) {
            throw new BadRequestException("Character does not belong to this campaign");
        }
        if (character.getOwner() == null || !character.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can only interact through your own characters");
        }
        return character;
    }

    private boolean isGmOrAdmin(UUID campaignId, User user) {
        return user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId());
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
