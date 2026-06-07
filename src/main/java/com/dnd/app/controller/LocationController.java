package com.dnd.app.controller;

import com.dnd.app.dto.request.*;
import com.dnd.app.dto.response.*;
import com.dnd.app.service.LocationService;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/locations")
@RequiredArgsConstructor
@Tag(name = "Locations", description = "Campaign location management")
public class LocationController {

    private final LocationService locationService;
    private final Executor controllerTaskExecutor;

    @PostMapping
    @Operation(summary = "Create location (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<LocationResponse>>> createLocation(
            @PathVariable UUID campaignId,
            @Valid @RequestBody CreateLocationRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            LocationResponse response = locationService.createLocation(campaignId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Location created"));
        }, controllerTaskExecutor);
    }

    @GetMapping
    @Operation(summary = "List locations")
    public CompletableFuture<ResponseEntity<ApiResponse<List<LocationResponse>>>> listLocations(
            @PathVariable UUID campaignId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<LocationResponse> locations = locationService.listLocations(campaignId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(locations));
        }, controllerTaskExecutor);
    }

    @GetMapping("/{locationId}")
    @Operation(summary = "Get location details")
    public CompletableFuture<ResponseEntity<ApiResponse<LocationResponse>>> getLocation(
            @PathVariable UUID campaignId,
            @PathVariable UUID locationId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            LocationResponse response = locationService.getLocation(locationId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response));
        }, controllerTaskExecutor);
    }

    @PutMapping("/{locationId}")
    @Operation(summary = "Update location (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<LocationResponse>>> updateLocation(
            @PathVariable UUID campaignId,
            @PathVariable UUID locationId,
            @Valid @RequestBody UpdateLocationRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            LocationResponse response = locationService.updateLocation(locationId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Location updated"));
        }, controllerTaskExecutor);
    }

    @DeleteMapping("/{locationId}")
    @Operation(summary = "Delete location (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteLocation(
            @PathVariable UUID campaignId,
            @PathVariable UUID locationId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            locationService.deleteLocation(locationId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Location deleted"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{locationId}/toggle-visibility")
    @Operation(summary = "Toggle location visibility (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<LocationResponse>>> toggleVisibility(
            @PathVariable UUID campaignId,
            @PathVariable UUID locationId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            LocationResponse response = locationService.toggleVisibility(locationId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Visibility toggled"));
        }, controllerTaskExecutor);
    }
}
