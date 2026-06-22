package com.dnd.app.service;

import com.dnd.app.dto.response.UserResponse;

/**
 * Result of a login/refresh: the access token (also returned in the body for the
 * WebSocket STOMP handshake), the refresh token (cookie-only), the access TTL, and
 * the authenticated user. Never serialized to the wire as-is — the controller copies
 * only the access token + expiry + user into {@link com.dnd.app.dto.response.AuthResponse}.
 */
public record IssuedTokens(
        String accessToken,
        String refreshToken,
        long accessExpiresInMs,
        long refreshExpiresInMs,
        UserResponse user) {
}
