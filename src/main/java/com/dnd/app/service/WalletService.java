package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.request.ModifyCurrencyRequest;
import com.dnd.app.dto.response.PageResponse;
import com.dnd.app.dto.response.WalletEntryResponse;
import com.dnd.app.dto.response.WalletHistoryEntryResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final CharacterWalletRepository characterWalletRepository;
    private final CurrencyTypeRepository currencyTypeRepository;
    private final PlayerCharacterRepository playerCharacterRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WebSocketEventService webSocketEventService;

    @Transactional(readOnly = true)
    public List<WalletEntryResponse> getWallet(UUID characterId, String username) {
        User user = getUser(username);
        PlayerCharacter character = findCharacter(characterId);
        enforceViewAccess(character, user);

        return characterWalletRepository.findByCharacterId(characterId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WalletEntryResponse modifyCurrency(UUID characterId, ModifyCurrencyRequest request, String username) {
        User user = getUser(username);
        PlayerCharacter character = findCharacter(characterId);
        enforceOwnerOrGmOrAdmin(character, user);

        UUID currencyTypeId = request.getCurrencyTypeId();
        BigDecimal delta = request.getAmount();

        // Atomic in-place update guarded by `amount + delta >= 0`.
        // Returns 1 when a row exists and the new balance stays non-negative; 0 otherwise.
        int updated = characterWalletRepository.applyDelta(characterId, currencyTypeId, delta);

        CharacterWallet wallet;
        if (updated == 1) {
            wallet = characterWalletRepository
                    .findByCharacterIdAndCurrencyTypeId(characterId, currencyTypeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Wallet entry not found for this currency type"));
        } else if (characterWalletRepository
                .findByCharacterIdAndCurrencyTypeId(characterId, currencyTypeId).isPresent()) {
            // Row exists but the update was rejected → would have driven the balance negative.
            throw new BadRequestException("Insufficient funds for this operation");
        } else if (delta.signum() < 0) {
            // No row yet and this is a debit → nothing to deduct from.
            throw new BadRequestException("Insufficient funds for this operation");
        } else {
            // First credit for this currency → create the wallet entry on demand.
            CurrencyType currencyType = currencyTypeRepository.findById(currencyTypeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Currency type not found"));
            wallet = characterWalletRepository.save(CharacterWallet.builder()
                    .character(character)
                    .currencyType(currencyType)
                    .amount(delta)
                    .build());
        }

        WalletTransaction transaction = WalletTransaction.builder()
                .character(character)
                .currencyType(wallet.getCurrencyType())
                .delta(delta)
                .balanceAfter(wallet.getAmount())
                .performedBy(username)
                .build();
        walletTransactionRepository.save(transaction);

        WalletEntryResponse response = toResponse(wallet);
        UUID campaignId = character.getCampaign() != null ? character.getCampaign().getId() : null;
        webSocketEventService.sendCampaignEvent(
                WebSocketEventType.WALLET_CHANGED, campaignId, characterId, response, user.getId());

        log.info("Currency modified: characterId={}, currencyTypeId={}, delta={}, newAmount={}, by={}",
                characterId, currencyTypeId, delta, wallet.getAmount(), username);
        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<WalletHistoryEntryResponse> getWalletHistory(UUID characterId, Pageable pageable, String username) {
        User user = getUser(username);
        PlayerCharacter character = findCharacter(characterId);
        enforceViewAccess(character, user);

        Page<WalletHistoryEntryResponse> page = walletTransactionRepository
                .findByCharacterIdOrderByCreatedAtDesc(characterId, pageable)
                .map(this::toHistoryResponse);
        return PageResponse.of(page);
    }

    @Transactional
    public void initializeDefaultWallet(PlayerCharacter character) {
        // Create Gold wallet entry with 0 amount
        CurrencyType gold = currencyTypeRepository.findBySlugAndHomebrewIsNull("gp")
                .orElse(null);

        if (gold != null) {
            CharacterWallet wallet = CharacterWallet.builder()
                    .character(character)
                    .currencyType(gold)
                    .amount(BigDecimal.ZERO)
                    .build();
            characterWalletRepository.save(wallet);
            log.info("Default wallet initialized: characterId={}, currency=Gold", character.getId());
        } else {
            log.warn("Gold currency type not found. Default wallet not initialized for characterId={}",
                    character.getId());
        }
    }

    // --- Private helpers ---

    private void enforceViewAccess(PlayerCharacter character, User user) {
        if (user.getRole() == Role.ADMIN) return;
        if (character.getCampaign() != null) {
            if (!campaignService.isMemberOfCampaign(character.getCampaign().getId(), user.getId())) {
                throw new AccessDeniedException("You are not a member of this character's campaign");
            }
        } else {
            if (!character.getOwner().getId().equals(user.getId())) {
                throw new AccessDeniedException("You cannot view this character's wallet");
            }
        }
    }

    private void enforceOwnerOrGmOrAdmin(PlayerCharacter character, User user) {
        if (user.getRole() == Role.ADMIN) return;
        if (character.getOwner().getId().equals(user.getId())) return;
        if (character.getCampaign() != null
                && campaignService.isGmInCampaign(character.getCampaign().getId(), user.getId())) return;
        throw new AccessDeniedException("Only the character owner, campaign GM, or ADMIN can modify currency");
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private PlayerCharacter findCharacter(UUID characterId) {
        return playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
    }

    private WalletEntryResponse toResponse(CharacterWallet wallet) {
        BigDecimal goldEquivalent = null;
        BigDecimal copperValue = wallet.getCurrencyType().getCopperValue();
        if (copperValue != null) {
            goldEquivalent = wallet.getAmount().multiply(copperValue)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        }

        return WalletEntryResponse.builder()
                .currencyTypeId(wallet.getCurrencyType().getId())
                .currencyName(wallet.getCurrencyType().getNameRu())
                .amount(wallet.getAmount())
                .goldEquivalent(goldEquivalent)
                .build();
    }

    private WalletHistoryEntryResponse toHistoryResponse(WalletTransaction transaction) {
        return WalletHistoryEntryResponse.builder()
                .id(transaction.getId())
                .currencyTypeId(transaction.getCurrencyType().getId())
                .currencyName(transaction.getCurrencyType().getNameRu())
                .delta(transaction.getDelta())
                .balanceAfter(transaction.getBalanceAfter())
                .reason(transaction.getReason())
                .performedBy(transaction.getPerformedBy())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
