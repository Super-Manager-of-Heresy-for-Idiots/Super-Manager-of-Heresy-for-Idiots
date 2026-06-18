package com.dnd.app.controller;

import com.dnd.app.dto.request.*;
import com.dnd.app.dto.response.*;
import com.dnd.app.service.HomebrewLibraryService;
import com.dnd.app.service.RaceService;
import com.dnd.app.service.homebrew.HomebrewAuthoringService;
import com.dnd.app.service.homebrew.HomebrewMarketplaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/api/homebrew")
@RequiredArgsConstructor
@Tag(name = "Homebrew", description = "Homebrew package management")
@org.springframework.validation.annotation.Validated
public class HomebrewController {

    private final HomebrewAuthoringService authoringService;
    private final HomebrewMarketplaceService marketplaceService;
    private final HomebrewLibraryService libraryService;
    private final RaceService raceService;
    private final Executor controllerTaskExecutor;

    // === Authoring (own packages) ===

    @PostMapping
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> createPackage(
            @Valid @RequestBody CreateHomebrewRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewDetailResponse data = authoringService.createPackage(request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(data, "Пакет создан"));
        }, controllerTaskExecutor);
    }

    @GetMapping("/my")
    public CompletableFuture<ResponseEntity<ApiResponse<Page<HomebrewPackageResponse>>>> listMyPackages(
            @RequestParam(required = false) String status,
            Pageable pageable, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            Page<HomebrewPackageResponse> data = authoringService.listMyPackages(auth.getName(), status, pageable);
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    @GetMapping("/my/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> getMyPackage(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewDetailResponse data = authoringService.getMyPackage(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    @PutMapping("/my/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> updatePackage(
            @PathVariable UUID id, @Valid @RequestBody UpdateHomebrewRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewDetailResponse data = authoringService.updatePackage(id, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Пакет обновлен"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/my/{id}/content")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> addContent(
            @PathVariable UUID id, @Valid @RequestBody AddContentRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewDetailResponse data = authoringService.addContent(id, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(data, "Контент добавлен"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/my/{packageId}/content/item-types")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> createPackageItemType(
            @PathVariable UUID packageId, @Valid @RequestBody CreateItemTypeRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewDetailResponse data = authoringService.createPackageItemType(packageId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(data, "Тип предмета добавлен в пакет"));
        }, controllerTaskExecutor);
    }

    // Legacy class-authoring endpoints (create/rich/import/update) removed in Phase 12 —
    // superseded by the aggregate ClassAuthoringController
    // (POST/PUT/DELETE /api/homebrew/packages/{packageId}/classes).

    @PostMapping("/my/{packageId}/content/skills")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> createPackageSkill(
            @PathVariable UUID packageId, @Valid @RequestBody CreateSkillRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewDetailResponse data = authoringService.createPackageSkill(packageId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(data, "Умение добавлено в пакет"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/my/{packageId}/content/feats")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> createPackageFeat(
            @PathVariable UUID packageId, @Valid @RequestBody CreateFeatRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewDetailResponse data = authoringService.createPackageFeat(packageId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(data, "Черта добавлена в пакет"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/my/{packageId}/content/buffs-debuffs")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> createPackageBuffDebuff(
            @PathVariable UUID packageId, @Valid @RequestBody CreateBuffDebuffRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewDetailResponse data = authoringService.createPackageBuffDebuff(packageId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(data, "Бафф/дебафф добавлен в пакет"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/my/{packageId}/content/races")
    public CompletableFuture<ResponseEntity<ApiResponse<RaceResponse>>> createPackageRace(
            @PathVariable UUID packageId, @Valid @RequestBody RaceCreateRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            RaceResponse data = raceService.createHomebrewRace(packageId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(data, "Homebrew race created"));
        }, controllerTaskExecutor);
    }

    @PutMapping("/my/{packageId}/content/races/{raceId}")
    public CompletableFuture<ResponseEntity<ApiResponse<RaceResponse>>> updatePackageRace(
            @PathVariable UUID packageId,
            @PathVariable UUID raceId,
            @Valid @RequestBody RaceUpdateRequest request,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(raceService.updateHomebrewRace(packageId, raceId, request, auth.getName()), "Homebrew race updated")),
                controllerTaskExecutor);
    }

    @PostMapping("/my/{packageId}/content/races/{raceId}/enable")
    public CompletableFuture<ResponseEntity<ApiResponse<RaceResponse>>> enablePackageRace(
            @PathVariable UUID packageId,
            @PathVariable UUID raceId,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(raceService.setHomebrewRaceActive(packageId, raceId, true, auth.getName()), "Homebrew race enabled")),
                controllerTaskExecutor);
    }

    @PostMapping("/my/{packageId}/content/races/{raceId}/disable")
    public CompletableFuture<ResponseEntity<ApiResponse<RaceResponse>>> disablePackageRace(
            @PathVariable UUID packageId,
            @PathVariable UUID raceId,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(raceService.setHomebrewRaceActive(packageId, raceId, false, auth.getName()), "Homebrew race disabled")),
                controllerTaskExecutor);
    }

    @PostMapping("/my/{packageId}/content/races/{raceId}/duplicate")
    public CompletableFuture<ResponseEntity<ApiResponse<RaceResponse>>> duplicateSystemRaceIntoPackage(
            @PathVariable UUID packageId,
            @PathVariable UUID raceId,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            RaceResponse data = raceService.duplicateSystemRaceIntoHomebrew(packageId, raceId, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(data, "System race duplicated into homebrew"));
        }, controllerTaskExecutor);
    }

    @DeleteMapping("/my/{id}/content/{contentItemId}")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> removeContent(
            @PathVariable UUID id, @PathVariable UUID contentItemId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            authoringService.removeContent(id, contentItemId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Контент удален"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/my/{id}/publish")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> publish(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewDetailResponse data = authoringService.publish(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Пакет опубликован"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/my/{id}/unpublish")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> unpublish(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewDetailResponse data = authoringService.unpublish(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Пакет снят с публикации"));
        }, controllerTaskExecutor);
    }

    @DeleteMapping("/my/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<Map<String, Object>>>> softDelete(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> data = authoringService.softDelete(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    // === Marketplace ===

    @GetMapping("/marketplace")
    public CompletableFuture<ResponseEntity<ApiResponse<Page<HomebrewPackageResponse>>>> browseMarketplace(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(0) int page,
            @RequestParam(defaultValue = "20") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) int size,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            Page<HomebrewPackageResponse> data = marketplaceService.browseMarketplace(
                    search, tags, sort, page, size, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    @GetMapping("/marketplace/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> getMarketplacePackage(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewDetailResponse data = marketplaceService.getMarketplacePackage(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    @PostMapping("/marketplace/{id}/install")
    public CompletableFuture<ResponseEntity<ApiResponse<Map<String, Object>>>> install(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> data = marketplaceService.installPackage(id, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(data, "Пакет установлен"));
        }, controllerTaskExecutor);
    }

    // === Installed ===

    @GetMapping("/installed")
    public CompletableFuture<ResponseEntity<ApiResponse<Page<InstalledHomebrewResponse>>>> listInstalled(
            Pageable pageable, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            Page<InstalledHomebrewResponse> data = marketplaceService.listInstalled(auth.getName(), pageable);
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    @DeleteMapping("/installed/{installationId}")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> uninstall(
            @PathVariable UUID installationId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            marketplaceService.uninstall(installationId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Пакет удален из установленных"));
        }, controllerTaskExecutor);
    }

    // === Ratings ===

    @PostMapping("/marketplace/{id}/rate")
    @Operation(summary = "Rate a homebrew package (like/dislike)")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewRatingResponse>>> ratePackage(
            @PathVariable UUID id,
            @Valid @RequestBody RateHomebrewRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewRatingResponse response = marketplaceService.ratePackage(id, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Rating submitted"));
        }, controllerTaskExecutor);
    }

    @GetMapping("/marketplace/{id}/rating")
    @Operation(summary = "Get package rating")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewRatingResponse>>> getPackageRating(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewRatingResponse response = marketplaceService.getPackageRating(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response));
        }, controllerTaskExecutor);
    }

    // === GM Library ===

    @GetMapping("/library")
    @Operation(summary = "List GM homebrew library")
    public CompletableFuture<ResponseEntity<ApiResponse<List<HomebrewPackageResponse>>>> listLibrary(Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<HomebrewPackageResponse> library = libraryService.listLibrary(auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(library));
        }, controllerTaskExecutor);
    }

    @PostMapping("/library/{packageId}")
    @Operation(summary = "Add package to GM library")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> addToLibrary(
            @PathVariable UUID packageId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            libraryService.addToLibrary(packageId, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(null, "Package added to library"));
        }, controllerTaskExecutor);
    }

    @DeleteMapping("/library/{packageId}")
    @Operation(summary = "Remove package from GM library")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> removeFromLibrary(
            @PathVariable UUID packageId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            libraryService.removeFromLibrary(packageId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Package removed from library"));
        }, controllerTaskExecutor);
    }
}
