package com.dnd.app.service;

import com.dnd.app.domain.RefreshToken;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.LoginRequest;
import com.dnd.app.dto.request.RegisterRequest;
import com.dnd.app.dto.response.UserResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.mapper.UserMapper;
import com.dnd.app.repository.RefreshTokenRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_USER_AGENT_LEN = 512;
    private static final int MAX_IP_LEN = 128;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

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

    @Transactional
    public IssuedTokens login(LoginRequest request, String userAgent, String ip) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        log.info("User logged in: username={}, role={}", user.getUsername(), user.getRole());
        // A login opens a fresh rotation family.
        return rotate(user, UUID.randomUUID(), null, userAgent, ip);
    }

    /**
     * Silently renews a session from a valid refresh token. The token is rotated: the presented
     * row is consumed (revoked + replaced_by) and a fresh token with a new jti is issued, so the
     * cookie keeps extending while the user stays active. Re-presenting an already-rotated token
     * means it was captured and replayed — the whole family is revoked. The user's role is re-read
     * so a role change takes effect on renewal.
     *
     * {@code noRollbackFor} keeps the theft-response family revoke committed even though we then
     * throw to reject the request.
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

    /** Revokes the session family behind a refresh token. Best-effort and never throws. */
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
            log.debug("Logout refresh-token revocation skipped: {}", e.getMessage());
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

        String access = tokenProvider.generateToken(user.getUsername(), role);
        String refresh = tokenProvider.generateRefreshToken(user.getUsername(), role, newJti.toString());
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
