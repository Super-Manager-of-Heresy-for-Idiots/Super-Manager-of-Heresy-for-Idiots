package com.dnd.app.controller;

import com.dnd.app.config.SecurityConfig;
import com.dnd.app.dto.request.LoginRequest;
import com.dnd.app.dto.request.RegisterRequest;
import com.dnd.app.dto.response.UserResponse;
import com.dnd.app.security.AuthCookieService;
import com.dnd.app.security.JwtTokenProvider;
import com.dnd.app.service.AuthService;
import com.dnd.app.service.IssuedTokens;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, AuthCookieService.class})
@DisplayName("AuthController: регистрация, вход, продление, выход")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthService authService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @DisplayName("Регистрация с валидными данными возвращает 201")
    void register_validRequest_returns201() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .username("testuser").email("test@test.com").password("password123").role("PLAYER").build();
        UserResponse resp = UserResponse.builder().username("testuser").build();
        when(authService.register(any())).thenReturn(resp);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    @DisplayName("Регистрация с невалидными данными возвращает 400")
    void register_invalidRequest_returns400() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .username("ab").email("invalid").password("short").role("INVALID").build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("Вход возвращает 200, токен в теле и две HttpOnly-cookie")
    void login_validRequest_returns200() throws Exception {
        LoginRequest req = LoginRequest.builder().username("user1").password("pass1234").build();
        UserResponse userResp = UserResponse.builder().username("user1").role("PLAYER").build();
        IssuedTokens tokens = new IssuedTokens("access-token", "refresh-token", 3600000L, 604800000L, userResp);
        when(authService.login(any(), any(), any())).thenReturn(tokens);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("access-token"))
                .andExpect(cookie().value("access_token", "access-token"))
                .andExpect(cookie().httpOnly("access_token", true))
                .andExpect(cookie().value("refresh_token", "refresh-token"))
                .andExpect(cookie().httpOnly("refresh_token", true))
                // refresh cookie is path-scoped to the auth endpoints
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.hasItem(containsString("refresh_token=refresh-token"))));
    }

    @Test
    @DisplayName("Продление по refresh-cookie возвращает новый access-токен")
    void refresh_withCookie_returnsNewToken() throws Exception {
        UserResponse userResp = UserResponse.builder().username("user1").role("PLAYER").build();
        IssuedTokens tokens = new IssuedTokens("new-access", "new-refresh", 3600000L, 604800000L, userResp);
        when(authService.refresh(eq("old-refresh"), any(), any())).thenReturn(tokens);

        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", "old-refresh")))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("new-access"))
                .andExpect(cookie().value("access_token", "new-access"))
                .andExpect(cookie().value("refresh_token", "new-refresh"));
    }

    @Test
    @DisplayName("Выход очищает обе session-cookie")
    void logout_clearsCookies() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(cookie().maxAge("access_token", 0))
                .andExpect(cookie().maxAge("refresh_token", 0));
    }
}
