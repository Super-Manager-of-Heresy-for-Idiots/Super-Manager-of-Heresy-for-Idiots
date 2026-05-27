package com.dnd.app.controller;

import com.dnd.app.config.SecurityConfig;
import com.dnd.app.dto.request.CreateCharacterRequest;
import com.dnd.app.dto.response.CharacterResponse;
import com.dnd.app.security.JwtTokenProvider;
import com.dnd.app.service.CharacterService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CharacterController.class)
@Import(SecurityConfig.class)
class CharacterControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private CharacterService characterService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(username = "player1", roles = "PLAYER")
    void createCharacter_returns201() throws Exception {
        CreateCharacterRequest req = CreateCharacterRequest.builder()
                .name("Hero").classId(UUID.randomUUID()).raceId(UUID.randomUUID()).build();
        CharacterResponse resp = CharacterResponse.builder().name("Hero").totalLevel(1).build();
        when(characterService.createCharacter(any(), eq("player1"))).thenReturn(resp);

        mockMvc.perform(post("/api/characters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Hero"));
    }

    @Test
    @WithMockUser(username = "player1", roles = "PLAYER")
    void listCharacters_returns200() throws Exception {
        when(characterService.listCharacters("player1")).thenReturn(List.of());

        mockMvc.perform(get("/api/characters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listCharacters_unauthenticated_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/characters"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "player1", roles = "PLAYER")
    void deleteCharacter_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/api/characters/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
