package com.dnd.app.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("AuthRateLimitFilter: ключ rate-limit устойчив к спуфингу X-Forwarded-For")
class AuthRateLimitFilterTest {

    private MockHttpServletResponse post(AuthRateLimitFilter filter, String path, String xff)
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr("127.0.0.1");
        if (xff != null) {
            request.addHeader("X-Forwarded-For", xff);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        // Fresh chain per call: a thrown-through (allowed) request leaves status at the 200 default.
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    @DisplayName("Подделка левого хопа XFF не создаёт новый бакет — лимит по реальному клиенту")
    void spoofedLeftHopSharesBucket() throws Exception {
        // loginPerMinute=2, one trusted proxy in front.
        AuthRateLimitFilter filter = new AuthRateLimitFilter(2, 3, 20, 1);

        // Same real client (rightmost hop appended by our proxy), different spoofed left hops.
        assertEquals(200, post(filter, "/api/auth/login", "evilA, 9.9.9.9").getStatus());
        assertEquals(200, post(filter, "/api/auth/login", "evilB, 9.9.9.9").getStatus());
        // Third hit from the same real client is throttled despite the changing left hop.
        assertEquals(429, post(filter, "/api/auth/login", "evilC, 9.9.9.9").getStatus());
    }

    @Test
    @DisplayName("Разные реальные клиенты получают раздельные бакеты")
    void distinctRealClientsAreIndependent() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(1, 3, 20, 1);

        assertEquals(200, post(filter, "/api/auth/login", "proxy, 1.1.1.1").getStatus());
        // Different real client → its own bucket, still allowed though the first one is full.
        assertEquals(200, post(filter, "/api/auth/login", "proxy, 2.2.2.2").getStatus());
        // Repeat first client → now throttled.
        assertEquals(429, post(filter, "/api/auth/login", "proxy, 1.1.1.1").getStatus());
    }

    @Test
    @DisplayName("Эндпоинт refresh теперь под rate-limit")
    void refreshIsRateLimited() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(5, 3, 1, 1); // refreshPerMinute=1

        assertEquals(200, post(filter, "/api/auth/refresh", "x, 8.8.8.8").getStatus());
        assertEquals(429, post(filter, "/api/auth/refresh", "x, 8.8.8.8").getStatus());
    }
}
