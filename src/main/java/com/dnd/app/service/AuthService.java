package com.dnd.app.service;

import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.LoginRequest;
import com.dnd.app.dto.request.RegisterRequest;
import com.dnd.app.dto.response.AuthResponse;
import com.dnd.app.dto.response.UserResponse;
import com.dnd.app.exception.BadRequestException;
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
