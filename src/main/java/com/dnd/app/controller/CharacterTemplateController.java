package com.dnd.app.controller;

import com.dnd.app.dto.request.CreateFullCharacterRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.CharacterResponse;
import com.dnd.app.service.CharacterService;
import com.dnd.app.service.CharacterWizardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
@Tag(name = "Character Templates", description = "Vanilla character templates (outside campaigns)")
public class CharacterTemplateController {

    private final CharacterService characterService;
    private final CharacterWizardService characterWizardService;

    @PostMapping("/full")
    @Operation(summary = "Create a vanilla character template via wizard (no campaign)")
    public ResponseEntity<ApiResponse<CharacterResponse>> createVanillaCharacter(
            @Valid @RequestBody CreateFullCharacterRequest request, Authentication auth) {
        CharacterResponse character = characterWizardService.createVanillaCharacter(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(character, "Шаблон персонажа создан"));
    }

    @GetMapping("/my")
    @Operation(summary = "List current user's template characters (not bound to any campaign)")
    public ResponseEntity<ApiResponse<List<CharacterResponse>>> listMyTemplates(Authentication auth) {
        List<CharacterResponse> templates = characterService.listTemplates(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(templates));
    }

    @GetMapping("/{characterId}")
    @Operation(summary = "Get a template character by ID")
    public ResponseEntity<ApiResponse<CharacterResponse>> getTemplate(
            @PathVariable UUID characterId, Authentication auth) {
        CharacterResponse character = characterService.getTemplateById(characterId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(character));
    }

    @DeleteMapping("/{characterId}")
    @Operation(summary = "Delete a template character")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @PathVariable UUID characterId, Authentication auth) {
        characterService.deleteTemplate(characterId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Шаблон персонажа удален"));
    }
}
