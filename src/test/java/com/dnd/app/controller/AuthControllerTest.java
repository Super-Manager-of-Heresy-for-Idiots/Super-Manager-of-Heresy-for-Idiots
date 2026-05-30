package com.dnd.app.controller;

import com.dnd.app.config.SecurityConfig;
import com.dnd.app.dto.request.LoginRequest;
import com.dnd.app.dto.request.RegisterRequest;
import com.dnd.app.dto.response.AuthResponse;
import com.dnd.app.dto.response.UserResponse;
import com.dnd.app.security.JwtAuthenticationFilter;
import com.dnd.app.security.JwtTokenProvider;
import com.dnd.app.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthService authService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    void register_validRequest_returns201() throws Exception {
        RegisterRequest req = RegisterRequest.builder()
                .username("testuser").email("test@test.com").password("password123").role("PLAYER").build();
        UserResponse resp = UserResponse.builder().username("testuser").build();
        when(authService.register(any())).thenReturn(resp);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
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
    void login_validRequest_returns200() throws Exception {
        LoginRequest req = LoginRequest.builder().username("user1").password("pass1234").build();
        UserResponse userResp = UserResponse.builder().username("user1").role("PLAYER").build();
        AuthResponse resp = AuthResponse.builder().token("jwt-token").expiresIn(86400000L).user(userResp).build();
        when(authService.login(any())).thenReturn(resp);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt-token"));
    }
}
