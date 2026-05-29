package com.dnd.app.controller;

import com.dnd.app.dto.request.AddEnchantmentRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.EnchantmentResponse;
import com.dnd.app.service.EnchantmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/characters/{characterId}/inventory/{slotId}/enchantments")
@RequiredArgsConstructor
public class EnchantmentController {

    private final EnchantmentService enchantmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EnchantmentResponse>>> getSlotEnchantments(
            @PathVariable UUID characterId, @PathVariable UUID slotId, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                enchantmentService.getSlotEnchantments(characterId, slotId, auth.getName())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EnchantmentResponse>> addEnchantment(
            @PathVariable UUID characterId, @PathVariable UUID slotId,
            @Valid @RequestBody AddEnchantmentRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        enchantmentService.addEnchantment(characterId, slotId, request, auth.getName()),
                        "Зачарование наложено"));
    }

    @DeleteMapping("/{enchantmentId}")
    public ResponseEntity<ApiResponse<Void>> removeEnchantment(
            @PathVariable UUID characterId, @PathVariable UUID slotId,
            @PathVariable UUID enchantmentId, Authentication auth) {
        enchantmentService.removeEnchantment(characterId, enchantmentId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Зачарование снято"));
    }
}
