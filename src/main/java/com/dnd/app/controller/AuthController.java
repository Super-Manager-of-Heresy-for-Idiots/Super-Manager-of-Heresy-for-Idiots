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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final Executor controllerTaskExecutor;

    @PostMapping("/register")
    public CompletableFuture<ResponseEntity<ApiResponse<UserResponse>>> register(@Valid @RequestBody RegisterRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Registration attempt: username={}, email={}, role={}",
                    request.getUsername(),
                    request.getEmail(),
                    request.getRole());
            long startTime = System.currentTimeMillis();

            UserResponse user = authService.register(request);

            log.info("Registration successful: username={}, userId={}, role={}, durationMs={}",
                    user.getUsername(),
                    user.getId(),
                    user.getRole(),
                    System.currentTimeMillis() - startTime);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(user, "Регистрация успешна"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/login")
    public CompletableFuture<ResponseEntity<ApiResponse<AuthResponse>>> login(@Valid @RequestBody LoginRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            log.info("Login attempt: username={}", request.getUsername());
            long startTime = System.currentTimeMillis();

            AuthResponse auth = authService.login(request);

            log.info("Login successful: username={}, userId={}, role={}, tokenIssued=true, durationMs={}",
                    auth.getUser().getUsername(),
                    auth.getUser().getId(),
                    auth.getUser().getRole(),
                    System.currentTimeMillis() - startTime);

            return ResponseEntity.ok(ApiResponse.ok(auth, "Вход выполнен"));
        }, controllerTaskExecutor);
    }
}
