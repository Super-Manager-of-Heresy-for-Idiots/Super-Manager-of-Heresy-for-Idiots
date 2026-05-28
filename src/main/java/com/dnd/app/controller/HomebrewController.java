package com.dnd.app.controller;

import com.dnd.app.dto.request.AddContentRequest;
import com.dnd.app.dto.request.CreateHomebrewRequest;
import com.dnd.app.dto.request.UpdateHomebrewRequest;
import com.dnd.app.dto.response.*;
import com.dnd.app.service.homebrew.HomebrewAuthoringService;
import com.dnd.app.service.homebrew.HomebrewMarketplaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/homebrew")
@RequiredArgsConstructor
public class HomebrewController {

    private final HomebrewAuthoringService authoringService;
    private final HomebrewMarketplaceService marketplaceService;

    // === Authoring (own packages) ===

    @PostMapping
    public ResponseEntity<ApiResponse<HomebrewDetailResponse>> createPackage(
            @Valid @RequestBody CreateHomebrewRequest request, Authentication auth) {
        HomebrewDetailResponse data = authoringService.createPackage(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(data, "Package created"));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<HomebrewPackageResponse>>> listMyPackages(
            @RequestParam(required = false) String status,
            Pageable pageable, Authentication auth) {
        Page<HomebrewPackageResponse> data = authoringService.listMyPackages(auth.getName(), status, pageable);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/my/{id}")
    public ResponseEntity<ApiResponse<HomebrewDetailResponse>> getMyPackage(
            @PathVariable UUID id, Authentication auth) {
        HomebrewDetailResponse data = authoringService.getMyPackage(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PutMapping("/my/{id}")
    public ResponseEntity<ApiResponse<HomebrewDetailResponse>> updatePackage(
            @PathVariable UUID id, @Valid @RequestBody UpdateHomebrewRequest request, Authentication auth) {
        HomebrewDetailResponse data = authoringService.updatePackage(id, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(data, "Package updated"));
    }

    @PostMapping("/my/{id}/content")
    public ResponseEntity<ApiResponse<HomebrewDetailResponse>> addContent(
            @PathVariable UUID id, @Valid @RequestBody AddContentRequest request, Authentication auth) {
        HomebrewDetailResponse data = authoringService.addContent(id, request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(data, "Content added"));
    }

    @DeleteMapping("/my/{id}/content/{contentItemId}")
    public ResponseEntity<ApiResponse<Void>> removeContent(
            @PathVariable UUID id, @PathVariable UUID contentItemId, Authentication auth) {
        authoringService.removeContent(id, contentItemId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Content removed"));
    }

    @PostMapping("/my/{id}/publish")
    public ResponseEntity<ApiResponse<HomebrewDetailResponse>> publish(
            @PathVariable UUID id, Authentication auth) {
        HomebrewDetailResponse data = authoringService.publish(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(data, "Package published"));
    }

    @PostMapping("/my/{id}/unpublish")
    public ResponseEntity<ApiResponse<HomebrewDetailResponse>> unpublish(
            @PathVariable UUID id, Authentication auth) {
        HomebrewDetailResponse data = authoringService.unpublish(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(data, "Package unpublished"));
    }

    @DeleteMapping("/my/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> softDelete(
            @PathVariable UUID id, Authentication auth) {
        Map<String, Object> data = authoringService.softDelete(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    // === Marketplace ===

    @GetMapping("/marketplace")
    public ResponseEntity<ApiResponse<Page<HomebrewPackageResponse>>> browseMarketplace(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        Page<HomebrewPackageResponse> data = marketplaceService.browseMarketplace(
                search, tags, sort, page, size, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/marketplace/{id}")
    public ResponseEntity<ApiResponse<HomebrewDetailResponse>> getMarketplacePackage(
            @PathVariable UUID id, Authentication auth) {
        HomebrewDetailResponse data = marketplaceService.getMarketplacePackage(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/marketplace/{id}/install")
    public ResponseEntity<ApiResponse<Map<String, Object>>> install(
            @PathVariable UUID id, Authentication auth) {
        Map<String, Object> data = marketplaceService.installPackage(id, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(data, "Package installed"));
    }

    // === Installed ===

    @GetMapping("/installed")
    public ResponseEntity<ApiResponse<Page<InstalledHomebrewResponse>>> listInstalled(
            Pageable pageable, Authentication auth) {
        Page<InstalledHomebrewResponse> data = marketplaceService.listInstalled(auth.getName(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @DeleteMapping("/installed/{installationId}")
    public ResponseEntity<ApiResponse<Void>> uninstall(
            @PathVariable UUID installationId, Authentication auth) {
        marketplaceService.uninstall(installationId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Package uninstalled"));
    }
}
