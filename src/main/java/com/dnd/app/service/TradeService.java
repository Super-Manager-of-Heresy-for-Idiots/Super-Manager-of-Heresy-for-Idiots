package com.dnd.app.service;

import com.dnd.app.domain.CampaignNpc;
import com.dnd.app.domain.CurrencyType;
import com.dnd.app.domain.ItemInstance;
import com.dnd.app.domain.ItemTemplate;
import com.dnd.app.domain.NpcShopItem;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.NpcRole;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.request.AddShopItemRequest;
import com.dnd.app.dto.request.BuyItemRequest;
import com.dnd.app.dto.request.ModifyCurrencyRequest;
import com.dnd.app.dto.request.SellItemRequest;
import com.dnd.app.dto.request.UpdateShopSettingsRequest;
import com.dnd.app.dto.response.ShopItemResponse;
import com.dnd.app.dto.response.ShopSettingsResponse;
import com.dnd.app.dto.response.TradeResultResponse;
import com.dnd.app.dto.response.WalletEntryResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CampaignNpcRepository;
import com.dnd.app.repository.CurrencyTypeRepository;
import com.dnd.app.repository.ItemInstanceRepository;
import com.dnd.app.repository.ItemTemplateRepository;
import com.dnd.app.repository.NpcShopItemRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Класс TradeService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeService {

    /**
     * Merchants buy back goods at half their gold price. Единственный источник истины —
     * фронтенд получает курс из API ({@code interact.buybackRatePercent}), а не хардкодит его.
     */
    public static final int BUYBACK_RATE_PERCENT = 50;
    private static final BigDecimal SELL_RATE =
            BigDecimal.valueOf(BUYBACK_RATE_PERCENT).divide(BigDecimal.valueOf(100));
    private static final String GOLD_SLUG = "gp";

    private final CampaignNpcRepository npcRepository;
    private final NpcShopItemRepository shopItemRepository;
    private final ItemTemplateRepository itemTemplateRepository;
    private final ItemInstanceRepository itemInstanceRepository;
    private final PlayerCharacterRepository characterRepository;
    private final CurrencyTypeRepository currencyTypeRepository;
    private final CampaignService campaignService;
    private final WalletService walletService;
    private final UserRepository userRepository;
    private final PresenceService presenceService;
    private final WebSocketEventService webSocketEventService;

    /**
     * Возвращает список для операции "list shop" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<ShopItemResponse> listShop(UUID campaignId, UUID npcId, String username) {
        User user = getUser(username);
        CampaignNpc npc = findMerchant(campaignId, npcId);
        campaignService.enforceMembershipOrAdmin(npc.getCampaign(), user);
        if (!isGmOrAdmin(campaignId, user) && !Boolean.TRUE.equals(npc.getIsVisibleToPlayers())) {
            throw new ResourceNotFoundException("NPC not found");
        }
        return shopItemRepository.findByNpcId(npc.getId()).stream().map(this::toResponse).toList();
    }

    /**
     * Выполняет операции "stock shop" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public ShopItemResponse stockShop(UUID campaignId, UUID npcId, AddShopItemRequest request, String username) {
        User user = getUser(username);
        CampaignNpc npc = findMerchant(campaignId, npcId);
        campaignService.enforceGmOrAdmin(npc.getCampaign(), user);

        ItemTemplate template = itemTemplateRepository.findById(request.getItemTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Item template not found"));

        NpcShopItem line = shopItemRepository.findByNpcIdAndItemTemplateIdForUpdate(npc.getId(), template.getId())
                .orElse(null);
        if (line == null) {
            line = NpcShopItem.builder()
                    .npc(npc)
                    .itemTemplate(template)
                    .priceGold(request.getPriceGold())
                    .quantity(request.getQuantity())
                    .restockQuantity(request.getRestockQuantity())
                    .build();
        } else {
            line.setQuantity(line.getQuantity() + request.getQuantity());
            if (request.getPriceGold() != null) {
                line.setPriceGold(request.getPriceGold());
            }
            if (Boolean.TRUE.equals(request.getClearRestockQuantity())) {
                line.setRestockQuantity(null);
            } else if (request.getRestockQuantity() != null) {
                line.setRestockQuantity(request.getRestockQuantity());
            }
        }
        line = shopItemRepository.save(line);
        log.info("Shop stocked: npcId={}, item='{}', qty+={}, by={}",
                npcId, template.getName(), request.getQuantity(), username);
        notifyShopUpdated(npc, user);
        return toResponse(line);
    }

    /**
     * Удаляет позицию из витрины торговца (ГМ). Полностью убирает строку стока по её id.
     * @param campaignId идентификатор кампании
     * @param npcId идентификатор NPC-торговца
     * @param shopItemId идентификатор позиции витрины
     * @param username имя пользователя, выполняющего операцию
     */
    @Transactional
    public void removeShopItem(UUID campaignId, UUID npcId, UUID shopItemId, String username) {
        User user = getUser(username);
        CampaignNpc npc = findMerchant(campaignId, npcId);
        campaignService.enforceGmOrAdmin(npc.getCampaign(), user);

        NpcShopItem line = shopItemRepository.findById(shopItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop item not found"));
        if (line.getNpc() == null || !line.getNpc().getId().equals(npc.getId())) {
            throw new ResourceNotFoundException("Shop item not found for this merchant");
        }
        shopItemRepository.delete(line);
        log.info("Shop item removed: npcId={}, shopItemId={}, by={}", npcId, shopItemId, username);
        notifyShopUpdated(npc, user);
    }

    /**
     * Восстанавливает запас витрины по базовым значениям (ГМ, WORLD_PLAN Этап 5). Позиции без
     * заданного restockQuantity не трогаются.
     * @param campaignId идентификатор кампании
     * @param npcId идентификатор NPC-торговца
     * @param username имя пользователя (мастер)
     * @return обновлённая витрина
     */
    @Transactional
    public List<ShopItemResponse> restockShop(UUID campaignId, UUID npcId, String username) {
        User user = getUser(username);
        CampaignNpc npc = findMerchant(campaignId, npcId);
        campaignService.enforceGmOrAdmin(npc.getCampaign(), user);

        List<NpcShopItem> lines = shopItemRepository.findByNpcId(npc.getId());
        int restocked = 0;
        for (NpcShopItem line : lines) {
            if (line.getRestockQuantity() != null) {
                line.setQuantity(line.getRestockQuantity());
                shopItemRepository.save(line);
                restocked++;
            }
        }
        log.info("Shop restocked: npcId={}, lines={}, by={}", npcId, restocked, username);
        notifyShopUpdated(npc, user);
        return lines.stream().map(this::toResponse).toList();
    }

    /**
     * Возвращает опциональные настройки экономики торговца (участники кампании).
     * @param campaignId идентификатор кампании
     * @param npcId идентификатор NPC-торговца
     * @param username имя пользователя
     * @return настройки (null-поля = не заданы)
     */
    @Transactional(readOnly = true)
    public ShopSettingsResponse getShopSettings(UUID campaignId, UUID npcId, String username) {
        User user = getUser(username);
        CampaignNpc npc = findMerchant(campaignId, npcId);
        campaignService.enforceMembershipOrAdmin(npc.getCampaign(), user);
        return toSettings(npc);
    }

    /**
     * Обновляет опциональные настройки экономики торговца (ГМ): кошелёк и модификатор цен.
     * Флаги clear* снимают соответствующее ограничение.
     * @param campaignId идентификатор кампании
     * @param npcId идентификатор NPC-торговца
     * @param request новые значения настроек
     * @param username имя пользователя (мастер)
     * @return актуальные настройки
     */
    @Transactional
    public ShopSettingsResponse updateShopSettings(UUID campaignId, UUID npcId,
                                                   UpdateShopSettingsRequest request, String username) {
        User user = getUser(username);
        CampaignNpc npc = findMerchant(campaignId, npcId);
        campaignService.enforceGmOrAdmin(npc.getCampaign(), user);

        if (Boolean.TRUE.equals(request.getClearMerchantGold())) {
            npc.setMerchantGold(null);
        } else if (request.getMerchantGold() != null) {
            npc.setMerchantGold(request.getMerchantGold());
        }
        if (Boolean.TRUE.equals(request.getClearPriceModifier())) {
            npc.setPriceModifierPercent(null);
        } else if (request.getPriceModifierPercent() != null) {
            npc.setPriceModifierPercent(request.getPriceModifierPercent());
        }
        npcRepository.save(npc);

        log.info("Shop settings updated: npcId={}, gold={}, modifier={}, by={}",
                npcId, npc.getMerchantGold(), npc.getPriceModifierPercent(), username);
        notifyShopUpdated(npc, user);
        return toSettings(npc);
    }

    /**
     * Итоговая цена позиции витрины с учётом опционального модификатора цен торговца.
     * Используется и при выдаче витрины, и при покупке, чтобы игрок платил ровно то, что видит.
     * @param line позиция витрины
     * @return цена за единицу или null, если цена не задана
     */
    public BigDecimal resolveUnitPrice(NpcShopItem line) {
        BigDecimal base = line.getPriceGold() != null ? line.getPriceGold()
                : line.getItemTemplate().getPriceGold();
        if (base == null) {
            return null;
        }
        Integer percent = line.getNpc() != null ? line.getNpc().getPriceModifierPercent() : null;
        if (percent == null || percent == 100) {
            return base;
        }
        return base.multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * Выполняет операции "buy" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public TradeResultResponse buy(UUID campaignId, UUID npcId, BuyItemRequest request, String username) {
        User user = getUser(username);
        CampaignNpc npc = findMerchant(campaignId, npcId);
        campaignService.enforceMembershipOrAdmin(npc.getCampaign(), user);

        PlayerCharacter character = resolveCampaignCharacter(request.getCharacterId(), campaignId, user);
        assertCanTrade(campaignId, character, npc, user);
        int qty = request.getQuantity();

        NpcShopItem line = shopItemRepository.findByNpcIdAndItemTemplateIdForUpdate(npc.getId(), request.getItemTemplateId())
                .orElseThrow(() -> new BadRequestException("This merchant does not sell that item"));
        if (line.getQuantity() < qty) {
            throw new BadRequestException("The merchant does not have enough of that item in stock");
        }

        BigDecimal unitPrice = resolveUnitPrice(line);
        if (unitPrice == null) {
            throw new BadRequestException("This item has no price and cannot be bought");
        }
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(qty));

        // Charge the buyer first so an insufficient balance aborts before any item is granted.
        WalletEntryResponse wallet = walletService.modifyCurrency(character.getId(),
                ModifyCurrencyRequest.builder().currencyTypeId(goldCurrencyId()).amount(total.negate()).build(),
                username);

        grantItem(character, line.getItemTemplate(), qty);

        // Позиции с базовым запасом сохраняем с нулевым остатком, иначе рестокинг потерял бы шаблон.
        if (line.getQuantity() == qty && line.getRestockQuantity() == null) {
            shopItemRepository.delete(line);
        } else {
            line.setQuantity(line.getQuantity() - qty);
            shopItemRepository.save(line);
        }

        // Опциональный кошелёк торговца: выручка пополняет его казну.
        if (npc.getMerchantGold() != null) {
            npc.setMerchantGold(npc.getMerchantGold().add(total));
        }

        log.info("Item bought: npcId={}, character={}, item='{}', qty={}, total={}, by={}",
                npcId, character.getId(), line.getItemTemplate().getName(), qty, total, username);
        notifyShopUpdated(npc, user);
        return TradeResultResponse.builder()
                .characterId(character.getId())
                .itemName(line.getItemTemplate().getName())
                .quantity(qty)
                .unitPriceGold(unitPrice)
                .totalPriceGold(total)
                .goldBalance(wallet.getAmount())
                .build();
    }

    /**
     * Выполняет операции "sell" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public TradeResultResponse sell(UUID campaignId, UUID npcId, SellItemRequest request, String username) {
        User user = getUser(username);
        CampaignNpc npc = findMerchant(campaignId, npcId);
        campaignService.enforceMembershipOrAdmin(npc.getCampaign(), user);

        PlayerCharacter character = resolveCampaignCharacter(request.getCharacterId(), campaignId, user);
        assertCanTrade(campaignId, character, npc, user);
        int qty = request.getQuantity();

        // Блокируем строку стека: без этого две конкурентные продажи одного стека
        // могли бы списать больше, чем есть (check-then-act по quantity).
        ItemInstance instance = itemInstanceRepository.findByIdForUpdate(request.getItemInstanceId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found"));
        if (instance.getOwnerCharacter() == null || !instance.getOwnerCharacter().getId().equals(character.getId())) {
            throw new BadRequestException("This item is not carried by that character");
        }
        ItemTemplate template = instance.getTemplate();
        if (template == null || template.getPriceGold() == null) {
            throw new BadRequestException("This item cannot be sold to a merchant");
        }
        int have = instance.getQuantity() != null ? instance.getQuantity() : 1;
        if (have < qty) {
            throw new BadRequestException("The character does not have that many of the item");
        }

        BigDecimal unitPrice = template.getPriceGold().multiply(SELL_RATE);
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(qty));

        // Опциональный кошелёк торговца: он не может купить дороже, чем у него есть золота.
        // Проверяем до изъятия товара, чтобы сделка отменилась целиком.
        if (npc.getMerchantGold() != null && npc.getMerchantGold().compareTo(total) < 0) {
            throw new BadRequestException("The merchant cannot afford to buy that");
        }

        // Remove the goods from the seller, then pay them.
        if (have == qty) {
            itemInstanceRepository.delete(instance);
        } else {
            instance.setQuantity(have - qty);
            itemInstanceRepository.save(instance);
        }
        WalletEntryResponse wallet = walletService.modifyCurrency(character.getId(),
                ModifyCurrencyRequest.builder().currencyTypeId(goldCurrencyId()).amount(total).build(),
                username);
        if (npc.getMerchantGold() != null) {
            npc.setMerchantGold(npc.getMerchantGold().subtract(total));
        }

        // Restock the merchant with what was sold.
        NpcShopItem line = shopItemRepository.findByNpcIdAndItemTemplateIdForUpdate(npc.getId(), template.getId()).orElse(null);
        if (line == null) {
            shopItemRepository.save(NpcShopItem.builder()
                    .npc(npc).itemTemplate(template).quantity(qty).build());
        } else {
            line.setQuantity(line.getQuantity() + qty);
            shopItemRepository.save(line);
        }

        log.info("Item sold: npcId={}, character={}, item='{}', qty={}, total={}, by={}",
                npcId, character.getId(), template.getName(), qty, total, username);
        notifyShopUpdated(npc, user);
        return TradeResultResponse.builder()
                .characterId(character.getId())
                .itemName(template.getName())
                .quantity(qty)
                .unitPriceGold(unitPrice)
                .totalPriceGold(total)
                .goldBalance(wallet.getAmount())
                .build();
    }

    // --- helpers ---

    /**
     * WORLD_PLAN Этап 3: игровой гейт торговли. Игрок торгует только своим персонажем,
     * стоящим в одной локации с видимым NPC-торговцем; ГМ торгует без ограничений.
     */
    private void assertCanTrade(UUID campaignId, PlayerCharacter character, CampaignNpc npc, User user) {
        if (isGmOrAdmin(campaignId, user)) {
            return;
        }
        if (!Boolean.TRUE.equals(npc.getIsVisibleToPlayers())) {
            throw new ResourceNotFoundException("NPC not found");
        }
        presenceService.assertSameLocation(character, npc);
    }

    private boolean isGmOrAdmin(UUID campaignId, User user) {
        return user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId());
    }

    /**
     * Оповещает подписчиков кампании, что витрина торговца изменилась, чтобы другие клиенты
     * (например, второй игрок у той же лавки) инвалидировали кэш и перечитали остатки/цены.
     */
    private void notifyShopUpdated(CampaignNpc npc, User actor) {
        if (npc.getCampaign() == null) {
            return;
        }
        webSocketEventService.sendCampaignEvent(
                WebSocketEventType.SHOP_UPDATED,
                npc.getCampaign().getId(),
                java.util.Map.of("npcId", npc.getId()),
                actor.getId());
    }

    private void grantItem(PlayerCharacter character, ItemTemplate template, int qty) {
        if (Boolean.TRUE.equals(template.getIsStackable())) {
            ItemInstance existing = itemInstanceRepository
                    .findStackableForCharacter(character.getId(), template.getId(), null, null)
                    .orElse(null);
            if (existing != null) {
                existing.setQuantity((existing.getQuantity() != null ? existing.getQuantity() : 0) + qty);
                itemInstanceRepository.save(existing);
                return;
            }
        }
        itemInstanceRepository.save(ItemInstance.builder()
                .template(template)
                .ownerCharacter(character)
                .quantity(qty)
                .build());
    }

    private CampaignNpc findMerchant(UUID campaignId, UUID npcId) {
        CampaignNpc npc = npcRepository.findById(npcId)
                .orElseThrow(() -> new ResourceNotFoundException("NPC not found"));
        if (npc.getCampaign() == null || !npc.getCampaign().getId().equals(campaignId)) {
            throw new ResourceNotFoundException("NPC not found in this campaign");
        }
        if (npc.getNpcRole() != NpcRole.MERCHANT) {
            throw new BadRequestException("This NPC is not a merchant");
        }
        return npc;
    }

    private PlayerCharacter resolveCampaignCharacter(UUID characterId, UUID campaignId, User user) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        if (character.getCampaign() == null || !character.getCampaign().getId().equals(campaignId)) {
            throw new BadRequestException("Character does not belong to this campaign");
        }
        boolean gm = user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId());
        if (!gm && (character.getOwner() == null || !character.getOwner().getId().equals(user.getId()))) {
            throw new AccessDeniedException("You can only trade with your own characters");
        }
        return character;
    }

    private UUID goldCurrencyId() {
        CurrencyType gold = currencyTypeRepository.findBySlugAndHomebrewIsNull(GOLD_SLUG)
                .orElseThrow(() -> new BadRequestException("Gold currency is not configured"));
        return gold.getId();
    }

    private ShopItemResponse toResponse(NpcShopItem line) {
        return ShopItemResponse.builder()
                .id(line.getId())
                .itemTemplateId(line.getItemTemplate().getId())
                .itemName(line.getItemTemplate().getName())
                .priceGold(resolveUnitPrice(line))
                .quantity(line.getQuantity())
                .restockQuantity(line.getRestockQuantity())
                .build();
    }

    private ShopSettingsResponse toSettings(CampaignNpc npc) {
        return ShopSettingsResponse.builder()
                .merchantGold(npc.getMerchantGold())
                .priceModifierPercent(npc.getPriceModifierPercent())
                .build();
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
