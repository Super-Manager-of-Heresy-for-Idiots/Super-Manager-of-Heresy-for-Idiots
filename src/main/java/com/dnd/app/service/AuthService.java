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
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration rejected — username already taken: {}", request.getUsername());
            throw new DuplicateResourceException("Имя пользователя уже занято");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration rejected — email already taken: {}", request.getEmail());
            throw new DuplicateResourceException("Эта электронная почта уже занята");
        }
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.valueOf(request.getRole()))
                .build();
        user = userRepository.save(user);
        log.info("User registered: username={}, role={}, id={}", user.getUsername(), user.getRole(), user.getId());
        return userMapper.toResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        String token = tokenProvider.generateToken(user.getUsername(), user.getRole().name());
        log.info("User logged in: username={}, role={}", user.getUsername(), user.getRole());
        return AuthResponse.builder()
                .token(token)
                .expiresIn(tokenProvider.getExpirationMs())
                .user(userMapper.toResponse(user))
                .build();
    }
}
