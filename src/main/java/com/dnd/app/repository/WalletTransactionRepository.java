package com.dnd.app.repository;

import com.dnd.app.domain.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    Page<WalletTransaction> findByCharacterIdOrderByCreatedAtDesc(UUID characterId, Pageable pageable);
}
