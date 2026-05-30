package com.dnd.app.controller;

import com.dnd.app.dto.request.AddBagItemRequest;
import com.dnd.app.dto.request.EquipFromBagRequest;
import com.dnd.app.dto.request.UpdateBagSlotRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.BagSlotResponse;
import com.dnd.app.service.BagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/characters/{characterId}/bag")
@RequiredArgsConstructor
public class BagController {

    private final BagService bagService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BagSlotResponse>>> listBag(
            @PathVariable UUID characterId, Authentication auth) {
        List<BagSlotResponse> bag = bagService.listBagContents(characterId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(bag));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BagSlotResponse>> addItem(
            @PathVariable UUID characterId,
            @Valid @RequestBody AddBagItemRequest request,
            Authentication auth) {
        BagSlotResponse slot = bagService.addItem(characterId, request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(slot, "Предмет добавлен в сумку"));
    }

    @PutMapping("/{slotId}")
    public ResponseEntity<ApiResponse<BagSlotResponse>> updateSlot(
            @PathVariable UUID characterId,
            @PathVariable UUID slotId,
            @Valid @RequestBody UpdateBagSlotRequest request,
            Authentication auth) {
        BagSlotResponse slot = bagService.updateSlot(characterId, slotId, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(slot, "Слот сумки обновлён"));
    }

    @DeleteMapping("/{slotId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @PathVariable UUID characterId,
            @PathVariable UUID slotId,
            Authentication auth) {
        bagService.removeItem(characterId, slotId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Предмет удалён из сумки"));
    }

    @PostMapping("/{bagSlotId}/equip")
    public ResponseEntity<ApiResponse<BagSlotResponse>> equipFromBag(
            @PathVariable UUID characterId,
            @PathVariable UUID bagSlotId,
            @Valid @RequestBody EquipFromBagRequest request,
            Authentication auth) {
        BagSlotResponse remaining = bagService.equipFromBag(characterId, bagSlotId, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(remaining, "Предмет экипирован"));
    }

    @PostMapping("/unequip/{equipmentSlot}")
    public ResponseEntity<ApiResponse<BagSlotResponse>> unequipToBag(
            @PathVariable UUID characterId,
            @PathVariable String equipmentSlot,
            Authentication auth) {
        BagSlotResponse slot = bagService.unequipToBag(characterId, equipmentSlot, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(slot, "Снаряжение снято в сумку"));
    }
}
