package com.dnd.app.controller;

import com.dnd.app.dto.request.CreateSharedStorageRequest;
import com.dnd.app.dto.response.*;
import com.dnd.app.service.SharedStorageService;
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
@RequestMapping("/api/campaigns/{campaignId}/shared-storage")
@RequiredArgsConstructor
@Tag(name = "Shared Storage", description = "Campaign shared storage management")
public class SharedStorageController {

    private final SharedStorageService sharedStorageService;

    @PostMapping
    @Operation(summary = "Create shared storage container (GM only)")
    public ResponseEntity<ApiResponse<SharedStorageResponse>> createStorage(
            @PathVariable UUID campaignId,
            @Valid @RequestBody CreateSharedStorageRequest request, Authentication auth) {
        SharedStorageResponse response = sharedStorageService.createStorage(campaignId, request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Storage created"));
    }

    @GetMapping
    @Operation(summary = "List shared storage containers")
    public ResponseEntity<ApiResponse<List<SharedStorageResponse>>> listStorages(
            @PathVariable UUID campaignId, Authentication auth) {
        List<SharedStorageResponse> storages = sharedStorageService.listStorages(campaignId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(storages));
    }

    @GetMapping("/{storageId}")
    @Operation(summary = "Get shared storage with items")
    public ResponseEntity<ApiResponse<SharedStorageResponse>> getStorage(
            @PathVariable UUID campaignId,
            @PathVariable UUID storageId, Authentication auth) {
        SharedStorageResponse response = sharedStorageService.getStorage(storageId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{storageId}")
    @Operation(summary = "Delete shared storage (GM only)")
    public ResponseEntity<ApiResponse<Void>> deleteStorage(
            @PathVariable UUID campaignId,
            @PathVariable UUID storageId, Authentication auth) {
        sharedStorageService.deleteStorage(storageId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Storage deleted"));
    }

    @PostMapping("/{storageId}/items/{instanceId}/deposit")
    @Operation(summary = "Deposit item into shared storage")
    public ResponseEntity<ApiResponse<Void>> depositItem(
            @PathVariable UUID campaignId,
            @PathVariable UUID storageId,
            @PathVariable UUID instanceId, Authentication auth) {
        sharedStorageService.addItemToStorage(storageId, instanceId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Item deposited"));
    }

    @PostMapping("/{storageId}/items/{instanceId}/take/{characterId}")
    @Operation(summary = "Take item from shared storage")
    public ResponseEntity<ApiResponse<Void>> takeItem(
            @PathVariable UUID campaignId,
            @PathVariable UUID storageId,
            @PathVariable UUID instanceId,
            @PathVariable UUID characterId, Authentication auth) {
        sharedStorageService.takeItemFromStorage(storageId, instanceId, characterId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Item taken"));
    }
}
