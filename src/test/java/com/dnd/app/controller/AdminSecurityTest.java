package com.dnd.app.controller;

import com.dnd.app.config.SecurityConfig;
import com.dnd.app.security.JwtTokenProvider;
import com.dnd.app.service.AdminService;
import com.dnd.app.service.homebrew.HomebrewAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminSecurityTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminService adminService;
    @MockitoBean private HomebrewAdminService homebrewAdminService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "GAME_MASTER")
    void adminItemTypesRemainAdminOnlyForGameMaster() throws Exception {
        mockMvc.perform(get("/api/admin/item-types"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminItemTypesAllowAdmin() throws Exception {
        when(adminService.listItemTypes()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/item-types"))
                .andExpect(status().isOk());
    }
}
