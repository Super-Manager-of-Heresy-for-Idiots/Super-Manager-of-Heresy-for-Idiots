package com.dnd.app.service;

import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.LoginRequest;
import com.dnd.app.dto.request.RegisterRequest;
import com.dnd.app.dto.response.UserResponse;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.mapper.UserMapper;
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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService: регистрация и вход")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserMapper userMapper;

    @InjectMocks private AuthService authService;

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
    @DisplayName("Успешный вход возвращает access и refresh токены")
    void login_success() {
        LoginRequest request = LoginRequest.builder().username("user1").password("pass123").build();
        User user = User.builder().id(UUID.randomUUID()).username("user1").role(Role.PLAYER).build();
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(tokenProvider.generateToken("user1", "PLAYER")).thenReturn("access-token");
        when(tokenProvider.generateRefreshToken("user1", "PLAYER")).thenReturn("refresh-token");
        when(tokenProvider.getExpirationMs()).thenReturn(3600000L);
        when(tokenProvider.getRefreshExpirationMs()).thenReturn(604800000L);
        UserResponse userResp = UserResponse.builder().username("user1").build();
        when(userMapper.toResponse(user)).thenReturn(userResp);

        IssuedTokens result = authService.login(request);

        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
        assertEquals(3600000L, result.accessExpiresInMs());
        assertEquals(604800000L, result.refreshExpiresInMs());
        assertEquals("user1", result.user().getUsername());
    }

    @Test
    @DisplayName("Вход с неверным паролем выбрасывает ошибку")
    void login_wrongPassword_throws() {
        LoginRequest request = LoginRequest.builder().username("user1").password("wrong").build();
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Продление по валидному refresh-токену выдаёт новую пару токенов")
    void refresh_validToken_rotatesTokens() {
        User user = User.builder().id(UUID.randomUUID()).username("user1").role(Role.PLAYER).build();
        when(tokenProvider.isRefreshToken("old-refresh")).thenReturn(true);
        when(tokenProvider.getUsernameFromToken("old-refresh")).thenReturn("user1");
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(tokenProvider.generateToken("user1", "PLAYER")).thenReturn("new-access");
        when(tokenProvider.generateRefreshToken("user1", "PLAYER")).thenReturn("new-refresh");
        when(tokenProvider.getExpirationMs()).thenReturn(3600000L);
        when(tokenProvider.getRefreshExpirationMs()).thenReturn(604800000L);
        when(userMapper.toResponse(user)).thenReturn(UserResponse.builder().username("user1").build());

        IssuedTokens result = authService.refresh("old-refresh");

        assertEquals("new-access", result.accessToken());
        assertEquals("new-refresh", result.refreshToken());
    }

    @Test
    @DisplayName("Продление с отсутствующим refresh-токеном выбрасывает ошибку")
    void refresh_nullToken_throws() {
        assertThrows(BadCredentialsException.class, () -> authService.refresh(null));
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    @DisplayName("Продление по access-токену вместо refresh выбрасывает ошибку")
    void refresh_nonRefreshToken_throws() {
        when(tokenProvider.isRefreshToken("access-token")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.refresh("access-token"));
        verify(userRepository, never()).findByUsername(anyString());
    }
}
