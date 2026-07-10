package com.dnd.app.service;

import com.dnd.app.domain.RefreshToken;
import com.dnd.app.domain.TrustedDeviceAccount;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.LoginRequest;
import com.dnd.app.dto.request.RegisterRequest;
import com.dnd.app.dto.response.UserResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.mapper.UserMapper;
import com.dnd.app.repository.RefreshTokenRepository;
import com.dnd.app.repository.TrustedDeviceAccountRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Класс AuthService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_USER_AGENT_LEN = 512;
    private static final int MAX_IP_LEN = 128;
    private static final long DEFAULT_TRUSTED_DEVICE_TTL_MS = 7_776_000_000L;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TrustedDeviceAccountRepository trustedDeviceAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @Value("${app.auth.trusted-device.ttl-ms:7776000000}")
    private long trustedDeviceTtlMs;

    /**
     * Выполняет операции "register" в рамках бизнес-логики домена.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        Role role;
        try {
            role = Role.valueOf(request.getRole());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException("Некорректная роль");
        }
        // Explicit block: ADMIN role cannot be created via public self-registration,
        // regardless of DTO validation rules. Admins are provisioned out-of-band.
        if (role == Role.ADMIN) {
            log.warn("Registration rejected — ADMIN role requested from public endpoint: username={}",
                    request.getUsername());
            throw new BadRequestException("Эта роль недоступна для самостоятельной регистрации");
        }

        // Unified message to prevent username/email enumeration via differing errors.
        boolean usernameTaken = userRepository.existsByUsername(request.getUsername());
        boolean emailTaken = userRepository.existsByEmail(request.getEmail());
        if (usernameTaken || emailTaken) {
            log.warn("Registration rejected — taken: usernameTaken={}, emailTaken={}",
                    usernameTaken, emailTaken);
            throw new DuplicateResourceException("Имя пользователя или email уже используются");
        }
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();
        user = userRepository.save(user);
        log.info("User registered: username={}, role={}, id={}", user.getUsername(), user.getRole(), user.getId());
        return userMapper.toResponse(user);
    }

    /**
     * Выполняет операции "login" в рамках бизнес-логики домена.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param userAgent заголовок клиента, используемый для аудита пользовательского обращения
     * @param ip IP-адрес клиента для аудита пользовательского обращения
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public IssuedTokens login(LoginRequest request, String userAgent, String ip) {
        return login(request, userAgent, ip, null);
    }

    @Transactional
    public IssuedTokens login(LoginRequest request, String userAgent, String ip, String trustedDeviceToken) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        log.info("User logged in: username={}, role={}", user.getUsername(), user.getRole());
        rememberTrustedDevice(trustedDeviceToken, user, userAgent, ip);
        // A login opens a fresh rotation family.
        return rotate(user, UUID.randomUUID(), null, userAgent, ip);
    }

    /**
     * Выполняет операции "refresh" в рамках бизнес-логики домена.
     * @param refreshToken входящее значение refresh token, используемое бизнес-сценарием
     * @param userAgent заголовок клиента, используемый для аудита пользовательского обращения
     * @param ip IP-адрес клиента для аудита пользовательского обращения
     * @return результат выполнения бизнес-операции
     */
    @Transactional(noRollbackFor = BadCredentialsException.class)
    public IssuedTokens refresh(String refreshToken, String userAgent, String ip) {
        if (refreshToken == null || !tokenProvider.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        UUID jti = parseJti(refreshToken);
        if (jti == null) {
            // Legacy refresh token minted before server-side sessions: force a re-login.
            throw new BadCredentialsException("Invalid refresh token");
        }
        RefreshToken row = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (row.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expired");
        }
        if (row.getReplacedBy() != null) {
            // Already rotated once — this is a replay of a consumed token. Treat as theft.
            refreshTokenRepository.revokeFamily(row.getFamilyId());
            log.warn("Refresh token reuse detected — revoking family. userId={}, familyId={}",
                    row.getUserId(), row.getFamilyId());
            throw new BadCredentialsException("Refresh token reuse detected");
        }
        if (row.isRevoked()) {
            throw new BadCredentialsException("Refresh token revoked");
        }

        User user = userRepository.findById(row.getUserId())
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        log.info("Session refreshed: username={}, role={}", user.getUsername(), user.getRole());
        return rotate(user, row.getFamilyId(), row, userAgent, ip);
    }

    /**
     * Выполняет операции "logout" в рамках бизнес-логики домена.
     * @param refreshToken входящее значение refresh token, используемое бизнес-сценарием
     */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null) {
            return;
        }
        try {
            UUID jti = parseJti(refreshToken);
            if (jti == null) {
                return;
            }
            refreshTokenRepository.findByJti(jti)
                    .ifPresent(row -> refreshTokenRepository.revokeFamily(row.getFamilyId()));
        } catch (RuntimeException e) {
            log.warn("AuthService#logout refresh-token revocation skipped: operation=logout-revoke-refresh-token", e);
        }
    }

    @Transactional
    public IssuedTokens switchTrustedDevice(UUID userId, String trustedDeviceToken, String userAgent, String ip) {
        TrustedDeviceAccount trustedAccount = requireTrustedAccount(userId, trustedDeviceToken);
        User user = userRepository.findById(trustedAccount.getUserId())
                .orElseThrow(() -> new BadCredentialsException("Trusted account not found"));

        Instant now = Instant.now();
        trustedAccount.setLastUsedAt(now);
        trustedAccount.setExpiresAt(now.plusMillis(trustedDeviceTtl()));
        trustedAccount.setUserAgent(truncate(userAgent, MAX_USER_AGENT_LEN));
        trustedAccount.setIp(truncate(ip, MAX_IP_LEN));
        trustedDeviceAccountRepository.save(trustedAccount);

        log.info("Trusted-device account switch: username={}, userId={}", user.getUsername(), user.getId());
        return rotate(user, UUID.randomUUID(), null, userAgent, ip);
    }

    @Transactional
    public void forgetTrustedDevice(UUID userId, String trustedDeviceToken) {
        if (userId == null || !StringUtils.hasText(trustedDeviceToken)) {
            return;
        }
        trustedDeviceAccountRepository.findByDeviceTokenHashAndUserId(hashDeviceToken(trustedDeviceToken), userId)
                .ifPresent(account -> {
                    account.setRevoked(true);
                    trustedDeviceAccountRepository.save(account);
                });
    }

    public String generateTrustedDeviceToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void rememberTrustedDevice(String trustedDeviceToken, User user, String userAgent, String ip) {
        if (!StringUtils.hasText(trustedDeviceToken) || user == null || user.getId() == null) {
            return;
        }
        Instant now = Instant.now();
        String hash = hashDeviceToken(trustedDeviceToken);
        TrustedDeviceAccount account = trustedDeviceAccountRepository
                .findByDeviceTokenHashAndUserId(hash, user.getId())
                .orElseGet(() -> TrustedDeviceAccount.builder()
                        .deviceTokenHash(hash)
                        .userId(user.getId())
                        .createdAt(now)
                        .build());
        account.setLastUsedAt(now);
        account.setExpiresAt(now.plusMillis(trustedDeviceTtl()));
        account.setRevoked(false);
        account.setUserAgent(truncate(userAgent, MAX_USER_AGENT_LEN));
        account.setIp(truncate(ip, MAX_IP_LEN));
        trustedDeviceAccountRepository.save(account);
    }

    private TrustedDeviceAccount requireTrustedAccount(UUID userId, String trustedDeviceToken) {
        if (userId == null || !StringUtils.hasText(trustedDeviceToken)) {
            throw new BadCredentialsException("Trusted device required");
        }
        TrustedDeviceAccount account = trustedDeviceAccountRepository
                .findByDeviceTokenHashAndUserId(hashDeviceToken(trustedDeviceToken), userId)
                .orElseThrow(() -> new BadCredentialsException("Trusted account not found"));
        if (account.isRevoked() || account.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Trusted account expired");
        }
        return account;
    }

    private long trustedDeviceTtl() {
        return trustedDeviceTtlMs > 0 ? trustedDeviceTtlMs : DEFAULT_TRUSTED_DEVICE_TTL_MS;
    }

    private static String hashDeviceToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /**
     * Mints a new access/refresh pair, persists the new refresh row, and — when rotating —
     * marks the previous row consumed pointing at its successor.
     */
    private IssuedTokens rotate(User user, UUID familyId, RefreshToken previous, String userAgent, String ip) {
        String role = user.getRole().name();
        UUID newJti = UUID.randomUUID();
        Instant now = Instant.now();
        RefreshToken row = RefreshToken.builder()
                .userId(user.getId())
                .jti(newJti)
                .familyId(familyId)
                .issuedAt(now)
                .expiresAt(now.plusMillis(tokenProvider.getRefreshExpirationMs()))
                .revoked(false)
                .userAgent(truncate(userAgent, MAX_USER_AGENT_LEN))
                .ip(truncate(ip, MAX_IP_LEN))
                .build();
        row = refreshTokenRepository.save(row);

        if (previous != null) {
            previous.setRevoked(true);
            previous.setReplacedBy(row.getId());
            refreshTokenRepository.save(previous);
        }

        String access = tokenProvider.generateToken(user.getUsername(), role, user.getId());
        String refresh = tokenProvider.generateRefreshToken(user.getUsername(), role, user.getId(), newJti.toString());
        return new IssuedTokens(
                access,
                refresh,
                tokenProvider.getExpirationMs(),
                tokenProvider.getRefreshExpirationMs(),
                userMapper.toResponse(user));
    }

    private UUID parseJti(String refreshToken) {
        String jtiStr = tokenProvider.getJti(refreshToken);
        if (jtiStr == null) {
            return null;
        }
        try {
            return UUID.fromString(jtiStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
