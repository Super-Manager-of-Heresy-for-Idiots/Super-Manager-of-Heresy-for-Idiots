package com.dnd.app.controller;

import com.dnd.app.config.SecurityConfig;
import com.dnd.app.dto.response.LoginPageStatsResponse;
import com.dnd.app.security.AuthCookieService;
import com.dnd.app.security.JwtTokenProvider;
import com.dnd.app.service.LoginPageStatsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicAppInfoController.class)
@Import({SecurityConfig.class, AuthCookieService.class})
@DisplayName("PublicAppInfoController")
class PublicAppInfoControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private LoginPageStatsService loginPageStatsService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/public/login-stats доступен без авторизации")
    void loginStats_isPublic() throws Exception {
        when(loginPageStatsService.getStats()).thenReturn(LoginPageStatsResponse.builder()
                .campaignCount(7L)
                .userCount(11L)
                .vigilDays(5L)
                .build());

        mockMvc.perform(get("/api/public/login-stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.campaignCount").value(7))
                .andExpect(jsonPath("$.data.userCount").value(11))
                .andExpect(jsonPath("$.data.vigilDays").value(5));
    }
}
