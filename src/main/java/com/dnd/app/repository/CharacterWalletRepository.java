package com.dnd.app.repository;

import com.dnd.app.domain.CharacterWallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CharacterWalletRepository extends JpaRepository<CharacterWallet, UUID> {

    List<CharacterWallet> findByCharacterId(UUID characterId);

    Optional<CharacterWallet> findByCharacterIdAndCurrencyTypeId(UUID characterId, UUID currencyTypeId);
}
