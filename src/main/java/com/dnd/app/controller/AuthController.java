package com.dnd.app.controller;

import com.dnd.app.dto.request.LoginRequest;
import com.dnd.app.dto.request.RegisterRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.AuthResponse;
import com.dnd.app.dto.response.UserResponse;
import com.dnd.app.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        // Логируем входные данные (без sensitive info)
        log.info("Registration attempt - username: {}, email: {}, role: {}",
                request.getUsername(),
                request.getEmail(),
                request.getRole());
        long startTime = System.currentTimeMillis();

        try {
            UserResponse user = authService.register(request);

            long duration = System.currentTimeMillis() - startTime;

            // Логируем успешную регистрацию
            log.info("Registration successful - username: {}, userId: {}, role: {}, duration: {}ms",
                    user.getUsername(),
                    user.getId(),
                    user.getRole(),
                    duration);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(user, "Registration successful"));

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            // Логируем ошибку регистрации
            log.error("Registration failed - username: {}, email: {}, role: {}, duration: {}ms, error: {}",
                    request.getUsername(),
                    request.getEmail(),
                    request.getRole(),
                    duration,
                    e.getMessage());

            throw e; // или обработать по-другому
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        // Логируем попытку входа (без пароля)
        log.info("Login attempt - username: {}", request.getUsername());

        long startTime = System.currentTimeMillis();

        try {
            AuthResponse auth = authService.login(request);

            long duration = System.currentTimeMillis() - startTime;

            // Логируем успешный вход
            log.info("Login successful - username: {}, userId: {}, role: {}, token issued, duration: {}ms",
                    auth.getUser().getUsername(),
                    auth.getUser().getId(),
                    auth.getUser().getRole(),
                    duration);

            // Для отладки - неполный токен (первые и последние символы)
            String tokenPreview = auth.getToken() != null && auth.getToken().length() > 20
                    ? auth.getToken().substring(0, 10) + "..." + auth.getToken().substring(auth.getToken().length() - 10)
                    : "N/A";
            log.debug("Token generated for user: {}, token preview: {}", request.getUsername(), tokenPreview);

            return ResponseEntity.ok(ApiResponse.ok(auth, "Login successful"));

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;

            // Логируем неудачную попытку входа с причиной
            log.warn("Login failed - username: {}, duration: {}ms, error: {}",
                    request.getUsername(),
                    duration,
                    e.getMessage());

            throw e; // или обработать по-другому
        }
    }
}
