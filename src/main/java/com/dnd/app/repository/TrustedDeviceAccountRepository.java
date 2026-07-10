package com.dnd.app.repository;

import com.dnd.app.domain.TrustedDeviceAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TrustedDeviceAccountRepository extends JpaRepository<TrustedDeviceAccount, UUID> {

    Optional<TrustedDeviceAccount> findByDeviceTokenHashAndUserId(String deviceTokenHash, UUID userId);
}
