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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Класс HomebrewController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequestMapping("/api/homebrew")
@RequiredArgsConstructor
@Tag(name = "Homebrew", description = "Homebrew package management")
@org.springframework.validation.annotation.Validated
public class HomebrewController {

    private final HomebrewAuthoringService authoringService;
    private final HomebrewMarketplaceService marketplaceService;
    private final HomebrewLibraryService libraryService;
    private final Executor controllerTaskExecutor;

    // === Authoring (own packages) ===

    /**
     * Создает результат операции "create package" в рамках бизнес-логики API.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> createPackage(
            @Valid @RequestBody CreateHomebrewRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewDetailResponse data = authoringService.createPackage(request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(data, "Пакет создан"));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает список для операции "list my packages" в рамках бизнес-логики API.
     * @param status входящее значение status, используемое бизнес-сценарием
     * @param pageable параметры постраничной выдачи для бизнес-списка
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/my")
    public CompletableFuture<ResponseEntity<ApiResponse<Page<HomebrewPackageResponse>>>> listMyPackages(
            @RequestParam(required = false) String status,
            Pageable pageable, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            Page<HomebrewPackageResponse> data = authoringService.listMyPackages(auth.getName(), status, pageable);
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get my package" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/my/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> getMyPackage(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewDetailResponse data = authoringService.getMyPackage(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    /**
     * Обновляет результат операции "update package" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PutMapping("/my/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> updatePackage(
            @PathVariable UUID id, @Valid @RequestBody UpdateHomebrewRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewDetailResponse data = authoringService.updatePackage(id, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Пакет обновлен"));
        }, controllerTaskExecutor);
    }

    /**
     * Добавляет результат операции "add content" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/my/{id}/content")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> addContent(
            @PathVariable UUID id, @Valid @RequestBody AddContentRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewDetailResponse data = authoringService.addContent(id, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(data, "Контент добавлен"));
        }, controllerTaskExecutor);
    }

    /**
     * Создает результат операции "create package item type" в рамках бизнес-логики API.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Создает результат операции "create package skill" в рамках бизнес-логики API.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/my/{packageId}/content/skills")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> createPackageSkill(
            @PathVariable UUID packageId, @Valid @RequestBody CreateSkillRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewDetailResponse data = authoringService.createPackageSkill(packageId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(data, "Умение добавлено в пакет"));
        }, controllerTaskExecutor);
    }

    /**
     * Создает результат операции "create package feat" в рамках бизнес-логики API.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/my/{packageId}/content/feats")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> createPackageFeat(
            @PathVariable UUID packageId, @Valid @RequestBody CreateFeatRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewDetailResponse data = authoringService.createPackageFeat(packageId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(data, "Черта добавлена в пакет"));
        }, controllerTaskExecutor);
    }

    /**
     * Создает результат операции "create package buff debuff" в рамках бизнес-логики API.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/my/{packageId}/content/buffs-debuffs")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> createPackageBuffDebuff(
            @PathVariable UUID packageId, @Valid @RequestBody CreateBuffDebuffRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewDetailResponse data = authoringService.createPackageBuffDebuff(packageId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(data, "Бафф/дебафф добавлен в пакет"));
        }, controllerTaskExecutor);
    }

    // Legacy homebrew race authoring endpoints removed in S5 — homebrew species attach to a
    // package via the reference system (SpeciesContentValidator, content type "SPECIES").

    /**
     * Удаляет результат операции "remove content" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param contentItemId идентификатор content item, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/my/{id}/content/{contentItemId}")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> removeContent(
            @PathVariable UUID id, @PathVariable UUID contentItemId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            authoringService.removeContent(id, contentItemId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Контент удален"));
        }, controllerTaskExecutor);
    }

    /**
     * Публикует событие операции "publish" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/my/{id}/publish")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> publish(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewDetailResponse data = authoringService.publish(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Пакет опубликован"));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "unpublish" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/my/{id}/unpublish")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> unpublish(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewDetailResponse data = authoringService.unpublish(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Пакет снят с публикации"));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "soft delete" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/my/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<Map<String, Object>>>> softDelete(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> data = authoringService.softDelete(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    // === Marketplace ===

    /**
     * Выполняет операции "browse marketplace" в рамках бизнес-логики API.
     * @param search входящее значение search, используемое бизнес-сценарием
     * @param tags входящее значение tags, используемое бизнес-сценарием
     * @param sort входящее значение sort, используемое бизнес-сценарием
     * @param page входящее значение page, используемое бизнес-сценарием
     * @param size входящее значение size, используемое бизнес-сценарием
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Возвращает результат операции "get marketplace package" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/marketplace/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> getMarketplacePackage(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewDetailResponse data = marketplaceService.getMarketplacePackage(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "install" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Возвращает список для операции "list installed" в рамках бизнес-логики API.
     * @param pageable параметры постраничной выдачи для бизнес-списка
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/installed")
    public CompletableFuture<ResponseEntity<ApiResponse<Page<InstalledHomebrewResponse>>>> listInstalled(
            Pageable pageable, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            Page<InstalledHomebrewResponse> data = marketplaceService.listInstalled(auth.getName(), pageable);
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "uninstall" в рамках бизнес-логики API.
     * @param installationId идентификатор installation, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/installed/{installationId}")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> uninstall(
            @PathVariable UUID installationId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            marketplaceService.uninstall(installationId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Пакет удален из установленных"));
        }, controllerTaskExecutor);
    }

    // === Ratings ===

    /**
     * Выполняет операции "rate package" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Возвращает результат операции "get package rating" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Возвращает список для операции "list library" в рамках бизнес-логики API.
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/library")
    @Operation(summary = "List GM homebrew library")
    public CompletableFuture<ResponseEntity<ApiResponse<List<HomebrewPackageResponse>>>> listLibrary(Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<HomebrewPackageResponse> library = libraryService.listLibrary(auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(library));
        }, controllerTaskExecutor);
    }

    /**
     * Добавляет результат операции "add to library" в рамках бизнес-логики API.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Удаляет результат операции "remove from library" в рамках бизнес-логики API.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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
