package com.dnd.app.controller;

import com.dnd.app.dto.request.*;
import com.dnd.app.dto.response.*;
import com.dnd.app.service.HomebrewLibraryService;
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

@RestController
@RequestMapping("/api/homebrew")
@RequiredArgsConstructor
@Tag(name = "Homebrew", description = "Homebrew package management")
public class HomebrewController {

    private final HomebrewAuthoringService authoringService;
    private final HomebrewMarketplaceService marketplaceService;
    private final HomebrewLibraryService libraryService;

    // === Authoring (own packages) ===

    @PostMapping
    public ResponseEntity<ApiResponse<HomebrewDetailResponse>> createPackage(
            @Valid @RequestBody CreateHomebrewRequest request, Authentication auth) {
        HomebrewDetailResponse data = authoringService.createPackage(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(data, "Пакет создан"));
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
        return ResponseEntity.ok(ApiResponse.ok(data, "Пакет обновлен"));
    }

    @PostMapping("/my/{id}/content")
    public ResponseEntity<ApiResponse<HomebrewDetailResponse>> addContent(
            @PathVariable UUID id, @Valid @RequestBody AddContentRequest request, Authentication auth) {
        HomebrewDetailResponse data = authoringService.addContent(id, request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(data, "Контент добавлен"));
    }

    @PostMapping("/my/{packageId}/content/item-types")
    public ResponseEntity<ApiResponse<HomebrewDetailResponse>> createPackageItemType(
            @PathVariable UUID packageId, @Valid @RequestBody CreateItemTypeRequest request, Authentication auth) {
        HomebrewDetailResponse data = authoringService.createPackageItemType(packageId, request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(data, "Тип предмета добавлен в пакет"));
    }

    @PostMapping("/my/{packageId}/content/classes")
    public ResponseEntity<ApiResponse<HomebrewDetailResponse>> createPackageCharacterClass(
            @PathVariable UUID packageId, @Valid @RequestBody CreateCharacterClassRequest request, Authentication auth) {
        HomebrewDetailResponse data = authoringService.createPackageCharacterClass(packageId, request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(data, "Класс добавлен в пакет"));
    }

    @PostMapping("/my/{packageId}/content/classes/rich")
    public ResponseEntity<ApiResponse<HomebrewClassCreationResponse>> createPackageCharacterClassRich(
            @PathVariable UUID packageId, @Valid @RequestBody CreateHomebrewClassRequest request, Authentication auth) {
        HomebrewClassCreationResponse data = authoringService.createPackageCharacterClassRich(packageId, request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(data, "Класс и награды уровней добавлены в пакет"));
    }

    @PostMapping("/my/{packageId}/content/classes/import-json")
    public ResponseEntity<ApiResponse<HomebrewClassCreationResponse>> importPackageCharacterClassJson(
            @PathVariable UUID packageId, @Valid @RequestBody CreateHomebrewClassRequest request, Authentication auth) {
        HomebrewClassCreationResponse data = authoringService.createPackageCharacterClassRich(packageId, request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(data, "Класс импортирован из JSON"));
    }

    @PutMapping("/my/{packageId}/content/classes/{classId}/rich")
    public ResponseEntity<ApiResponse<HomebrewClassCreationResponse>> updatePackageCharacterClassRich(
            @PathVariable UUID packageId,
            @PathVariable UUID classId,
            @Valid @RequestBody CreateHomebrewClassRequest request,
            Authentication auth) {
        HomebrewClassCreationResponse data = authoringService.updatePackageCharacterClassRich(packageId, classId, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(data, "Класс и награды уровней обновлены"));
    }

    @PostMapping("/my/{packageId}/content/skills")
    public ResponseEntity<ApiResponse<HomebrewDetailResponse>> createPackageSkill(
            @PathVariable UUID packageId, @Valid @RequestBody CreateSkillRequest request, Authentication auth) {
        HomebrewDetailResponse data = authoringService.createPackageSkill(packageId, request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(data, "Умение добавлено в пакет"));
    }

    @PostMapping("/my/{packageId}/content/feats")
    public ResponseEntity<ApiResponse<HomebrewDetailResponse>> createPackageFeat(
            @PathVariable UUID packageId, @Valid @RequestBody CreateFeatRequest request, Authentication auth) {
        HomebrewDetailResponse data = authoringService.createPackageFeat(packageId, request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(data, "Черта добавлена в пакет"));
    }

    @PostMapping("/my/{packageId}/content/buffs-debuffs")
    public ResponseEntity<ApiResponse<HomebrewDetailResponse>> createPackageBuffDebuff(
            @PathVariable UUID packageId, @Valid @RequestBody CreateBuffDebuffRequest request, Authentication auth) {
        HomebrewDetailResponse data = authoringService.createPackageBuffDebuff(packageId, request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(data, "Бафф/дебафф добавлен в пакет"));
    }

    @DeleteMapping("/my/{id}/content/{contentItemId}")
    public ResponseEntity<ApiResponse<Void>> removeContent(
            @PathVariable UUID id, @PathVariable UUID contentItemId, Authentication auth) {
        authoringService.removeContent(id, contentItemId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Контент удален"));
    }

    @PostMapping("/my/{id}/publish")
    public ResponseEntity<ApiResponse<HomebrewDetailResponse>> publish(
            @PathVariable UUID id, Authentication auth) {
        HomebrewDetailResponse data = authoringService.publish(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(data, "Пакет опубликован"));
    }

    @PostMapping("/my/{id}/unpublish")
    public ResponseEntity<ApiResponse<HomebrewDetailResponse>> unpublish(
            @PathVariable UUID id, Authentication auth) {
        HomebrewDetailResponse data = authoringService.unpublish(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(data, "Пакет снят с публикации"));
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
                .body(ApiResponse.ok(data, "Пакет установлен"));
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
        return ResponseEntity.ok(ApiResponse.ok(null, "Пакет удален из установленных"));
    }

    // === Ratings ===

    @PostMapping("/marketplace/{id}/rate")
    @Operation(summary = "Rate a homebrew package (like/dislike)")
    public ResponseEntity<ApiResponse<HomebrewRatingResponse>> ratePackage(
            @PathVariable UUID id,
            @Valid @RequestBody RateHomebrewRequest request, Authentication auth) {
        HomebrewRatingResponse response = marketplaceService.ratePackage(id, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Rating submitted"));
    }

    @GetMapping("/marketplace/{id}/rating")
    @Operation(summary = "Get package rating")
    public ResponseEntity<ApiResponse<HomebrewRatingResponse>> getPackageRating(
            @PathVariable UUID id, Authentication auth) {
        HomebrewRatingResponse response = marketplaceService.getPackageRating(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // === GM Library ===

    @GetMapping("/library")
    @Operation(summary = "List GM homebrew library")
    public ResponseEntity<ApiResponse<List<HomebrewPackageResponse>>> listLibrary(Authentication auth) {
        List<HomebrewPackageResponse> library = libraryService.listLibrary(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(library));
    }

    @PostMapping("/library/{packageId}")
    @Operation(summary = "Add package to GM library")
    public ResponseEntity<ApiResponse<Void>> addToLibrary(
            @PathVariable UUID packageId, Authentication auth) {
        libraryService.addToLibrary(packageId, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(null, "Package added to library"));
    }

    @DeleteMapping("/library/{packageId}")
    @Operation(summary = "Remove package from GM library")
    public ResponseEntity<ApiResponse<Void>> removeFromLibrary(
            @PathVariable UUID packageId, Authentication auth) {
        libraryService.removeFromLibrary(packageId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Package removed from library"));
    }
}
