package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.Role;
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

        // Pre-check existence (so we can distinguish 404 from "insufficient funds")
        characterWalletRepository
                .findByCharacterIdAndCurrencyTypeId(characterId, request.getCurrencyTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet entry not found for this currency type"));

        int updated = characterWalletRepository.applyDelta(
                characterId, request.getCurrencyTypeId(), request.getAmount());
        if (updated == 0) {
            throw new BadRequestException("Insufficient funds for this operation");
        }

        // Refresh canonical state after atomic update
        CharacterWallet wallet = characterWalletRepository
                .findByCharacterIdAndCurrencyTypeId(characterId, request.getCurrencyTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet entry not found for this currency type"));

        WalletTransaction transaction = WalletTransaction.builder()
                .character(character)
                .currencyType(wallet.getCurrencyType())
                .delta(request.getAmount())
                .balanceAfter(wallet.getAmount())
                .performedBy(username)
                .build();
        walletTransactionRepository.save(transaction);

        log.info("Currency modified: characterId={}, currencyTypeId={}, delta={}, newAmount={}, by={}",
                characterId, request.getCurrencyTypeId(), request.getAmount(), wallet.getAmount(), username);
        return toResponse(wallet);
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
        CurrencyType gold = currencyTypeRepository.findByNameIgnoreCase("Gold")
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
        if (wallet.getCurrencyType().getExchangeRateToGold() != null) {
            goldEquivalent = wallet.getAmount().multiply(wallet.getCurrencyType().getExchangeRateToGold());
        }

        return WalletEntryResponse.builder()
                .currencyTypeId(wallet.getCurrencyType().getId())
                .currencyName(wallet.getCurrencyType().getName())
                .amount(wallet.getAmount())
                .goldEquivalent(goldEquivalent)
                .build();
    }

    private WalletHistoryEntryResponse toHistoryResponse(WalletTransaction transaction) {
        return WalletHistoryEntryResponse.builder()
                .id(transaction.getId())
                .currencyTypeId(transaction.getCurrencyType().getId())
                .currencyName(transaction.getCurrencyType().getName())
                .delta(transaction.getDelta())
                .balanceAfter(transaction.getBalanceAfter())
                .reason(transaction.getReason())
                .performedBy(transaction.getPerformedBy())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
