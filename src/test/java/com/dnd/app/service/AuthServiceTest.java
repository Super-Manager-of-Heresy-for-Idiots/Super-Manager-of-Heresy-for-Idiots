package com.dnd.app.service;

import com.dnd.app.domain.RefreshToken;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.LoginRequest;
import com.dnd.app.dto.request.RegisterRequest;
import com.dnd.app.dto.response.UserResponse;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.mapper.UserMapper;
import com.dnd.app.repository.RefreshTokenRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService: регистрация, вход и ротация refresh-токенов")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserMapper userMapper;

    @InjectMocks private AuthService authService;

    /** save() returns the same row, assigning an id like GenerationType.UUID would. */
    private void stubSaveAssignsId() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken row = invocation.getArgument(0);
            if (row.getId() == null) {
                row.setId(UUID.randomUUID());
            }
            return row;
        });
    }

    @Test
    @DisplayName("Успешная регистрация нового пользователя")
    void register_success() {
        RegisterRequest request = RegisterRequest.builder()
                .username("testuser").email("test@test.com").password("password123").role("PLAYER").build();
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        User savedUser = User.builder().id(UUID.randomUUID()).username("testuser")
                .email("test@test.com").role(Role.PLAYER).build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        UserResponse expected = UserResponse.builder().username("testuser").build();
        when(userMapper.toResponse(savedUser)).thenReturn(expected);

        UserResponse result = authService.register(request);

        assertEquals("testuser", result.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Регистрация с занятым именем пользователя выбрасывает ошибку")
    void register_duplicateUsername_throws() {
        RegisterRequest request = RegisterRequest.builder()
                .username("taken").email("x@x.com").password("password123").role("PLAYER").build();
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Регистрация с занятым email выбрасывает ошибку")
    void register_duplicateEmail_throws() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser").email("taken@x.com").password("password123").role("PLAYER").build();
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("taken@x.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
    }

    @Test
    @DisplayName("Успешный вход открывает новую семью и сохраняет refresh-сессию")
    void login_success() {
        LoginRequest request = LoginRequest.builder().username("user1").password("pass123").build();
        User user = User.builder().id(UUID.randomUUID()).username("user1").role(Role.PLAYER).build();
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        stubSaveAssignsId();
        when(tokenProvider.generateToken("user1", "PLAYER", user.getId())).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken(eq("user1"), eq("PLAYER"), eq(user.getId()), anyString())).thenReturn("refresh-token");
        when(tokenProvider.getExpirationMs()).thenReturn(3600000L);
        when(tokenProvider.getRefreshExpirationMs()).thenReturn(604800000L);
        when(userMapper.toResponse(user)).thenReturn(UserResponse.builder().username("user1").build());

        IssuedTokens result = authService.login(request, "agent", "1.2.3.4");

        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
        assertEquals(3600000L, result.accessExpiresInMs());
        assertEquals(604800000L, result.refreshExpiresInMs());
        assertEquals("user1", result.user().getUsername());
        // First token of a login: persisted, no predecessor to consume.
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Вход с неверным паролем выбрасывает ошибку")
    void login_wrongPassword_throws() {
        LoginRequest request = LoginRequest.builder().username("user1").password("wrong").build();
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(BadCredentialsException.class, () -> authService.login(request, "agent", "1.2.3.4"));
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Продление валидным токеном ротирует: старая строка гасится и указывает на преемника")
    void refresh_validToken_rotatesAndConsumesPredecessor() {
        UUID jti = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        User user = User.builder().id(UUID.randomUUID()).username("user1").role(Role.PLAYER).build();
        RefreshToken row = RefreshToken.builder()
                .id(UUID.randomUUID()).userId(user.getId()).jti(jti).familyId(familyId)
                .issuedAt(Instant.now().minusSeconds(60)).expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false).build();

        when(tokenProvider.isRefreshToken("old-refresh")).thenReturn(true);
        when(tokenProvider.getJti("old-refresh")).thenReturn(jti.toString());
        when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(row));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        stubSaveAssignsId();
        when(tokenProvider.generateToken("user1", "PLAYER", user.getId())).thenReturn("new-access");
        when(tokenProvider.generateRefreshToken(eq("user1"), eq("PLAYER"), eq(user.getId()), anyString())).thenReturn("new-refresh");
        when(tokenProvider.getExpirationMs()).thenReturn(3600000L);
        when(tokenProvider.getRefreshExpirationMs()).thenReturn(604800000L);
        when(userMapper.toResponse(user)).thenReturn(UserResponse.builder().username("user1").build());

        IssuedTokens result = authService.refresh("old-refresh", "agent", "1.2.3.4");

        assertEquals("new-access", result.accessToken());
        assertEquals("new-refresh", result.refreshToken());
        assertTrue(row.isRevoked(), "предыдущая строка должна быть погашена");
        assertNotNull(row.getReplacedBy(), "предыдущая строка должна указывать на преемника");
        // New row + consumed predecessor.
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Повторное предъявление уже отротированного токена гасит всю семью и отклоняется")
    void refresh_reusedToken_revokesFamily() {
        UUID jti = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        RefreshToken consumed = RefreshToken.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID()).jti(jti).familyId(familyId)
                .issuedAt(Instant.now().minusSeconds(120)).expiresAt(Instant.now().plusSeconds(3600))
                .revoked(true).replacedBy(UUID.randomUUID()).build();

        when(tokenProvider.isRefreshToken("stolen")).thenReturn(true);
        when(tokenProvider.getJti("stolen")).thenReturn(jti.toString());
        when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(consumed));

        assertThrows(BadCredentialsException.class, () -> authService.refresh("stolen", "agent", "9.9.9.9"));
        verify(refreshTokenRepository, times(1)).revokeFamily(familyId);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Просроченная refresh-строка отклоняется без гашения семьи")
    void refresh_expiredRow_throws() {
        UUID jti = UUID.randomUUID();
        RefreshToken expired = RefreshToken.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID()).jti(jti).familyId(UUID.randomUUID())
                .issuedAt(Instant.now().minusSeconds(7200)).expiresAt(Instant.now().minusSeconds(60))
                .revoked(false).build();

        when(tokenProvider.isRefreshToken("expired")).thenReturn(true);
        when(tokenProvider.getJti("expired")).thenReturn(jti.toString());
        when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(expired));

        assertThrows(BadCredentialsException.class, () -> authService.refresh("expired", "agent", "1.2.3.4"));
        verify(refreshTokenRepository, never()).revokeFamily(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("Отозванная (logout) строка отклоняется без гашения семьи")
    void refresh_revokedRow_throws() {
        UUID jti = UUID.randomUUID();
        RefreshToken revoked = RefreshToken.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID()).jti(jti).familyId(UUID.randomUUID())
                .issuedAt(Instant.now().minusSeconds(60)).expiresAt(Instant.now().plusSeconds(3600))
                .revoked(true).build();

        when(tokenProvider.isRefreshToken("revoked")).thenReturn(true);
        when(tokenProvider.getJti("revoked")).thenReturn(jti.toString());
        when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(revoked));

        assertThrows(BadCredentialsException.class, () -> authService.refresh("revoked", "agent", "1.2.3.4"));
        verify(refreshTokenRepository, never()).revokeFamily(any());
    }

    @Test
    @DisplayName("Продление с отсутствующим refresh-токеном выбрасывает ошибку")
    void refresh_nullToken_throws() {
        assertThrows(BadCredentialsException.class, () -> authService.refresh(null, "agent", "1.2.3.4"));
        verify(refreshTokenRepository, never()).findByJti(any());
    }

    @Test
    @DisplayName("Продление по access-токену вместо refresh выбрасывает ошибку")
    void refresh_nonRefreshToken_throws() {
        when(tokenProvider.isRefreshToken("access-token")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.refresh("access-token", "agent", "1.2.3.4"));
        verify(refreshTokenRepository, never()).findByJti(any());
    }

    @Test
    @DisplayName("Logout гасит всю семью предъявленного токена")
    void logout_revokesFamily() {
        UUID jti = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();
        RefreshToken row = RefreshToken.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID()).jti(jti).familyId(familyId)
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).revoked(false).build();
        when(tokenProvider.getJti("sess")).thenReturn(jti.toString());
        when(refreshTokenRepository.findByJti(jti)).thenReturn(Optional.of(row));

        authService.logout("sess");

        verify(refreshTokenRepository, times(1)).revokeFamily(familyId);
    }

    @Test
    @DisplayName("Logout без токена — no-op, БД не трогается")
    void logout_nullToken_noop() {
        authService.logout(null);
        verifyNoInteractions(refreshTokenRepository);
    }
}
