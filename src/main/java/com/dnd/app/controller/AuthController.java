package com.dnd.app.controller;

import com.dnd.app.dto.request.LoginRequest;
import com.dnd.app.dto.request.RegisterRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.AuthResponse;
import com.dnd.app.dto.response.UserResponse;
import com.dnd.app.security.AuthCookieService;
import com.dnd.app.service.AuthService;
import com.dnd.app.service.IssuedTokens;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
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
    private final AuthCookieService cookieService;
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
    public CompletableFuture<ResponseEntity<ApiResponse<AuthResponse>>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        // Read request metadata on the servlet thread, before handing off to the async executor.
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        String ip = clientIp(httpRequest);
        return CompletableFuture.supplyAsync(() -> {
            log.info("Login attempt: username={}", request.getUsername());
            long startTime = System.currentTimeMillis();

            IssuedTokens tokens = authService.login(request, userAgent, ip);

            log.info("Login successful: username={}, userId={}, role={}, tokenIssued=true, durationMs={}",
                    tokens.user().getUsername(),
                    tokens.user().getId(),
                    tokens.user().getRole(),
                    System.currentTimeMillis() - startTime);

            return sessionResponse(tokens, "Вход выполнен");
        }, controllerTaskExecutor);
    }

    /**
     * Silent renewal endpoint. The browser sends the HttpOnly refresh cookie automatically;
     * we rotate it and hand back a fresh access token (body + cookie). Public: it authenticates
     * via the refresh cookie itself, not via an access token.
     */
    @PostMapping("/refresh")
    public CompletableFuture<ResponseEntity<ApiResponse<AuthResponse>>> refresh(
            @CookieValue(name = "${app.jwt.refresh-cookie-name:refresh_token}", required = false) String refreshToken,
            HttpServletRequest httpRequest) {
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        String ip = clientIp(httpRequest);
        return CompletableFuture.supplyAsync(() -> {
            IssuedTokens tokens = authService.refresh(refreshToken, userAgent, ip);
            log.info("Token refreshed: username={}", tokens.user().getUsername());
            return sessionResponse(tokens, "Сессия продлена");
        }, controllerTaskExecutor);
    }

    /**
     * Revokes the server-side session family and clears both cookies. Idempotent and public —
     * always succeeds even if the refresh cookie is absent or already revoked.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = "${app.jwt.refresh-cookie-name:refresh_token}", required = false) String refreshToken) {
        authService.logout(refreshToken);
        ResponseCookie clearAccess = cookieService.clearAccessCookie();
        ResponseCookie clearRefresh = cookieService.clearRefreshCookie();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearAccess.toString())
                .header(HttpHeaders.SET_COOKIE, clearRefresh.toString())
                .body(ApiResponse.ok(null, "Выход выполнен"));
    }

    /**
     * Best-effort client IP for session forensics only (stored, never used for a trust decision).
     * Prefers the X-Forwarded-For chain when present so it is meaningful behind a proxy.
     */
    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.trim();
        }
        return request.getRemoteAddr();
    }

    private ResponseEntity<ApiResponse<AuthResponse>> sessionResponse(IssuedTokens tokens, String message) {
        ResponseCookie accessCookie = cookieService.accessCookie(tokens.accessToken(), tokens.accessExpiresInMs());
        ResponseCookie refreshCookie = cookieService.refreshCookie(tokens.refreshToken(), tokens.refreshExpiresInMs());
        AuthResponse body = AuthResponse.builder()
                .token(tokens.accessToken())
                .expiresIn(tokens.accessExpiresInMs())
                .user(tokens.user())
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.ok(body, message));
    }
}
