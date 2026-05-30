package com.dnd.app.controller;

import com.dnd.app.dto.request.CreateCharacterRequest;
import com.dnd.app.dto.request.UpdateCharacterRequest;
import com.dnd.app.dto.request.UpdateInventorySlotRequest;
import com.dnd.app.dto.request.UpdateStatRequest;
import com.dnd.app.dto.response.*;
import com.dnd.app.service.CharacterService;
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
public class CharacterController {

    private final CharacterService characterService;

    @PostMapping
    public ResponseEntity<ApiResponse<CharacterResponse>> createCharacter(
            @Valid @RequestBody CreateCharacterRequest request, Authentication auth) {
        CharacterResponse character = characterService.createCharacter(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(character, "Персонаж создан"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CharacterResponse>>> listCharacters(
            @RequestParam(required = false) UUID teamId, Authentication auth) {
        List<CharacterResponse> characters = characterService.listCharacters(auth.getName(), teamId);
        return ResponseEntity.ok(ApiResponse.ok(characters));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CharacterResponse>> getCharacter(
            @PathVariable UUID id, Authentication auth) {
        CharacterResponse character = characterService.getCharacterById(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(character));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CharacterResponse>> updateCharacter(
            @PathVariable UUID id, @Valid @RequestBody UpdateCharacterRequest request, Authentication auth) {
        CharacterResponse character = characterService.updateCharacter(id, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(character, "Персонаж обновлен"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCharacter(@PathVariable UUID id, Authentication auth) {
        characterService.deleteCharacter(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Персонаж удален"));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<ApiResponse<List<CharacterStatResponse>>> getStats(
            @PathVariable UUID id, Authentication auth) {
        List<CharacterStatResponse> stats = characterService.getStats(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @PutMapping("/{id}/stats/{statId}")
    public ResponseEntity<ApiResponse<CharacterStatResponse>> updateStat(
            @PathVariable UUID id, @PathVariable UUID statId,
            @Valid @RequestBody UpdateStatRequest request, Authentication auth) {
        CharacterStatResponse stat = characterService.updateStatValue(id, statId, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(stat, "Характеристика обновлена"));
    }

    @GetMapping("/{id}/inventory")
    public ResponseEntity<ApiResponse<List<InventorySlotResponse>>> getInventory(
            @PathVariable UUID id, Authentication auth) {
        List<InventorySlotResponse> inventory = characterService.getInventory(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(inventory));
    }

    @PutMapping("/{id}/inventory/{slot}")
    public ResponseEntity<ApiResponse<InventorySlotResponse>> updateInventorySlot(
            @PathVariable UUID id, @PathVariable String slot,
            @Valid @RequestBody UpdateInventorySlotRequest request, Authentication auth) {
        InventorySlotResponse invSlot = characterService.updateInventorySlot(id, slot, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(invSlot, "Слот инвентаря обновлен"));
    }
}
