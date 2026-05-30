package com.dnd.app.controller;

import com.dnd.app.dto.request.AddEnchantmentRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.EnchantmentResponse;
import com.dnd.app.service.EnchantmentService;
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
@RequestMapping("/api/campaigns/{campaignId}/characters/{characterId}/inventory/{instanceId}/enchantments")
@RequiredArgsConstructor
@Tag(name = "Item Enchantments", description = "Enchantment management for item instances")
public class EnchantmentController {

    private final EnchantmentService enchantmentService;

    @GetMapping
    @Operation(summary = "Get enchantments on item instance")
    public ResponseEntity<ApiResponse<List<EnchantmentResponse>>> getItemEnchantments(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId,
            @PathVariable UUID instanceId, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                enchantmentService.getItemEnchantments(characterId, instanceId, auth.getName())));
    }

    @PostMapping
    @Operation(summary = "Add enchantment to item instance")
    public ResponseEntity<ApiResponse<EnchantmentResponse>> addEnchantment(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId,
            @PathVariable UUID instanceId,
            @Valid @RequestBody AddEnchantmentRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(
                        enchantmentService.addItemEnchantment(characterId, instanceId, request, auth.getName()),
                        "Зачарование наложено"));
    }

    @DeleteMapping("/{enchantmentId}")
    @Operation(summary = "Remove enchantment from item instance")
    public ResponseEntity<ApiResponse<Void>> removeEnchantment(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId,
            @PathVariable UUID instanceId,
            @PathVariable UUID enchantmentId, Authentication auth) {
        enchantmentService.removeItemEnchantment(characterId, enchantmentId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Зачарование снято"));
    }
}
