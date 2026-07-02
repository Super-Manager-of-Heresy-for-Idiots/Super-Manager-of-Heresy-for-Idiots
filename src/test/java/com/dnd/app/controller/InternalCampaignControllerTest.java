package com.dnd.app.controller;

import com.dnd.app.config.SecurityConfig;
import com.dnd.app.dto.response.CampaignAccessResponse;
import com.dnd.app.security.AuthCookieService;
import com.dnd.app.security.JwtTokenProvider;
import com.dnd.app.service.CampaignService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalCampaignController.class)
@Import({SecurityConfig.class, AuthCookieService.class})
@TestPropertySource(properties = "app.internal.api-key=test-internal-key")
@DisplayName("InternalCampaignController: service-to-service campaign access")
class InternalCampaignControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CampaignService campaignService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/internal/campaigns/{campaignId}/access returns flat map-service DTO")
    void getCampaignAccess_withInternalKey_returnsFlatDto() throws Exception {
        UUID campaignId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID characterId = UUID.randomUUID();
        when(campaignService.getCampaignAccess(campaignId, userId)).thenReturn(CampaignAccessResponse.builder()
                .campaignId(campaignId)
                .userId(userId)
                .canView(true)
                .canManageMaps(false)
                .canMoveAnyToken(false)
                .movableCharacterIds(List.of(characterId))
                .build());

        mockMvc.perform(get("/api/internal/campaigns/{campaignId}/access", campaignId)
                        .header("X-Internal-Api-Key", "test-internal-key")
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaignId").value(campaignId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.canView").value(true))
                .andExpect(jsonPath("$.canManageMaps").value(false))
                .andExpect(jsonPath("$.canMoveAnyToken").value(false))
                .andExpect(jsonPath("$.movableCharacterIds[0]").value(characterId.toString()))
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("internal endpoint rejects requests without service API key")
    void getCampaignAccess_withoutInternalKey_forbidden() throws Exception {
        mockMvc.perform(get("/api/internal/campaigns/{campaignId}/access", UUID.randomUUID())
                        .param("userId", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GM response exposes manage maps permissions")
    void getCampaignAccess_gm_canManageMaps() throws Exception {
        UUID campaignId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(campaignService.getCampaignAccess(campaignId, userId)).thenReturn(CampaignAccessResponse.builder()
                .campaignId(campaignId)
                .userId(userId)
                .canView(true)
                .canManageMaps(true)
                .canMoveAnyToken(true)
                .movableCharacterIds(List.of())
                .build());

        mockMvc.perform(get("/api/internal/campaigns/{campaignId}/access", campaignId)
                        .header("X-Internal-Api-Key", "test-internal-key")
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canView").value(true))
                .andExpect(jsonPath("$.canManageMaps").value(true))
                .andExpect(jsonPath("$.canMoveAnyToken").value(true));
    }
}
