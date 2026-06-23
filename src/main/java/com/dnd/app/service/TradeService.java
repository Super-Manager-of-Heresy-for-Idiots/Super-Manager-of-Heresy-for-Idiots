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
import com.dnd.app.dto.request.AddShopItemRequest;
import com.dnd.app.dto.request.BuyItemRequest;
import com.dnd.app.dto.request.ModifyCurrencyRequest;
import com.dnd.app.dto.request.SellItemRequest;
import com.dnd.app.dto.response.ShopItemResponse;
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
import java.util.List;
import java.util.UUID;

/**
 * Trading with merchant NPCs. The GM stocks a merchant's shop with item templates priced in gold;
 * players then buy from it (gold leaves the wallet, the item is granted) or sell carried items
 * back at a reduced rate. Currency moves through {@link WalletService} (which enforces ownership
 * and non-negative balances), so a purchase fails cleanly when the buyer cannot afford it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TradeService {

    /** Merchants buy back goods at half their gold price. */
    private static final BigDecimal SELL_RATE = new BigDecimal("0.5");
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

    @Transactional(readOnly = true)
    public List<ShopItemResponse> listShop(UUID campaignId, UUID npcId, String username) {
        User user = getUser(username);
        CampaignNpc npc = findMerchant(campaignId, npcId);
        campaignService.enforceMembershipOrAdmin(npc.getCampaign(), user);
        return shopItemRepository.findByNpcId(npc.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ShopItemResponse stockShop(UUID campaignId, UUID npcId, AddShopItemRequest request, String username) {
        User user = getUser(username);
        CampaignNpc npc = findMerchant(campaignId, npcId);
        campaignService.enforceGmOrAdmin(npc.getCampaign(), user);

        ItemTemplate template = itemTemplateRepository.findById(request.getItemTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Item template not found"));

        NpcShopItem line = shopItemRepository.findByNpcIdAndItemTemplateId(npc.getId(), template.getId())
                .orElse(null);
        if (line == null) {
            line = NpcShopItem.builder()
                    .npc(npc)
                    .itemTemplate(template)
                    .priceGold(request.getPriceGold())
                    .quantity(request.getQuantity())
                    .build();
        } else {
            line.setQuantity(line.getQuantity() + request.getQuantity());
            if (request.getPriceGold() != null) {
                line.setPriceGold(request.getPriceGold());
            }
        }
        line = shopItemRepository.save(line);
        log.info("Shop stocked: npcId={}, item='{}', qty+={}, by={}",
                npcId, template.getName(), request.getQuantity(), username);
        return toResponse(line);
    }

    @Transactional
    public TradeResultResponse buy(UUID campaignId, UUID npcId, BuyItemRequest request, String username) {
        User user = getUser(username);
        CampaignNpc npc = findMerchant(campaignId, npcId);
        campaignService.enforceMembershipOrAdmin(npc.getCampaign(), user);

        PlayerCharacter character = resolveCampaignCharacter(request.getCharacterId(), campaignId, user);
        int qty = request.getQuantity();

        NpcShopItem line = shopItemRepository.findByNpcIdAndItemTemplateId(npc.getId(), request.getItemTemplateId())
                .orElseThrow(() -> new BadRequestException("This merchant does not sell that item"));
        if (line.getQuantity() < qty) {
            throw new BadRequestException("The merchant does not have enough of that item in stock");
        }

        BigDecimal unitPrice = line.getPriceGold() != null ? line.getPriceGold()
                : line.getItemTemplate().getPriceGold();
        if (unitPrice == null) {
            throw new BadRequestException("This item has no price and cannot be bought");
        }
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(qty));

        // Charge the buyer first so an insufficient balance aborts before any item is granted.
        WalletEntryResponse wallet = walletService.modifyCurrency(character.getId(),
                ModifyCurrencyRequest.builder().currencyTypeId(goldCurrencyId()).amount(total.negate()).build(),
                username);

        grantItem(character, line.getItemTemplate(), qty);

        if (line.getQuantity() == qty) {
            shopItemRepository.delete(line);
        } else {
            line.setQuantity(line.getQuantity() - qty);
            shopItemRepository.save(line);
        }

        log.info("Item bought: npcId={}, character={}, item='{}', qty={}, total={}, by={}",
                npcId, character.getId(), line.getItemTemplate().getName(), qty, total, username);
        return TradeResultResponse.builder()
                .characterId(character.getId())
                .itemName(line.getItemTemplate().getName())
                .quantity(qty)
                .unitPriceGold(unitPrice)
                .totalPriceGold(total)
                .goldBalance(wallet.getAmount())
                .build();
    }

    @Transactional
    public TradeResultResponse sell(UUID campaignId, UUID npcId, SellItemRequest request, String username) {
        User user = getUser(username);
        CampaignNpc npc = findMerchant(campaignId, npcId);
        campaignService.enforceMembershipOrAdmin(npc.getCampaign(), user);

        PlayerCharacter character = resolveCampaignCharacter(request.getCharacterId(), campaignId, user);
        int qty = request.getQuantity();

        ItemInstance instance = itemInstanceRepository.findById(request.getItemInstanceId())
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

        // Restock the merchant with what was sold.
        NpcShopItem line = shopItemRepository.findByNpcIdAndItemTemplateId(npc.getId(), template.getId()).orElse(null);
        if (line == null) {
            shopItemRepository.save(NpcShopItem.builder()
                    .npc(npc).itemTemplate(template).quantity(qty).build());
        } else {
            line.setQuantity(line.getQuantity() + qty);
            shopItemRepository.save(line);
        }

        log.info("Item sold: npcId={}, character={}, item='{}', qty={}, total={}, by={}",
                npcId, character.getId(), template.getName(), qty, total, username);
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
        BigDecimal price = line.getPriceGold() != null ? line.getPriceGold()
                : line.getItemTemplate().getPriceGold();
        return ShopItemResponse.builder()
                .id(line.getId())
                .itemTemplateId(line.getItemTemplate().getId())
                .itemName(line.getItemTemplate().getName())
                .priceGold(price)
                .quantity(line.getQuantity())
                .build();
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
