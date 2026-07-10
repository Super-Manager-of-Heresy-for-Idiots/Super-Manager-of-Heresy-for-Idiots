package com.dnd.app.repository;

import com.dnd.app.domain.CharacterWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт CharacterWalletRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CharacterWalletRepository extends JpaRepository<CharacterWallet, UUID> {

    List<CharacterWallet> findByCharacterId(UUID characterId);

    Optional<CharacterWallet> findByCharacterIdAndCurrencyTypeId(UUID characterId, UUID currencyTypeId);

    /**
     * Atomic balance change that prevents over-withdraw under concurrent modifications.
     * Returns the number of rows updated: 1 = success, 0 = either wallet missing or amount + delta &lt; 0.
     */
    @Modifying
    @Query("update CharacterWallet w set w.amount = w.amount + :delta " +
            "where w.character.id = :characterId and w.currencyType.id = :currencyTypeId " +
            "and w.amount + :delta >= 0")
    int applyDelta(@Param("characterId") UUID characterId,
                   @Param("currencyTypeId") UUID currencyTypeId,
                   @Param("delta") BigDecimal delta);
}
