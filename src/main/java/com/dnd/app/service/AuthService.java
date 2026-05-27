package com.dnd.app.service;

import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.LoginRequest;
import com.dnd.app.dto.request.RegisterRequest;
import com.dnd.app.dto.response.AuthResponse;
import com.dnd.app.dto.response.UserResponse;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.mapper.UserMapper;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        log.debug("  AuthService.register: username={}", request.getUsername());
        if (userRepository.existsByUsername(request.getUsername())) {
            log.debug("  AuthService.register: username already exists");
            throw new DuplicateResourceException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.debug("  AuthService.register: email already exists");
            throw new DuplicateResourceException("Email already exists");
        }
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.valueOf(request.getRole()))
                .build();
        user = userRepository.save(user);
        log.debug("  AuthService.register: saved user id={}", user.getId());
        return userMapper.toResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        log.debug("  AuthService.login: authenticating username={}", request.getUsername());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            log.debug("  AuthService.login: authentication SUCCESS");
        } catch (Exception e) {
            log.debug("  AuthService.login: authentication FAILED: {}", e.getMessage());
            throw e;
        }
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        log.debug("  AuthService.login: user found, generating token");
        String token = tokenProvider.generateToken(user.getUsername(), user.getRole().name());
        log.debug("  AuthService.login: token generated, length={}", token.length());
        return AuthResponse.builder()
                .token(token)
                .expiresIn(tokenProvider.getExpirationMs())
                .user(userMapper.toResponse(user))
                .build();
    }
}
