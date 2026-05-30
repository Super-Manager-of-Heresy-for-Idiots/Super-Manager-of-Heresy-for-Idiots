package com.dnd.app.controller;

import com.dnd.app.config.SecurityConfig;
import com.dnd.app.dto.request.CreateTeamRequest;
import com.dnd.app.dto.request.JoinTeamRequest;
import com.dnd.app.dto.response.TeamResponse;
import com.dnd.app.security.JwtTokenProvider;
import com.dnd.app.service.TeamContentService;
import com.dnd.app.service.TeamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamController.class)
@Import(SecurityConfig.class)
class TeamControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private TeamService teamService;
    @MockitoBean private TeamContentService teamContentService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "gm1", roles = "GAME_MASTER")
    void createTeam_returns201() throws Exception {
        CreateTeamRequest req = CreateTeamRequest.builder().name("Party A").build();
        TeamResponse resp = TeamResponse.builder().name("Party A").build();
        when(teamService.createTeam(any(), eq("gm1"))).thenReturn(resp);

        mockMvc.perform(post("/api/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Party A"));
    }

    @Test
    @WithMockUser(username = "player1", roles = "PLAYER")
    void joinTeam_returns200() throws Exception {
        JoinTeamRequest req = JoinTeamRequest.builder().inviteCode("ABCD1234").build();
        TeamResponse resp = TeamResponse.builder().name("Party A").build();
        when(teamService.joinTeam(any(), eq("player1"))).thenReturn(resp);

        mockMvc.perform(post("/api/teams/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Party A"));
    }
}
