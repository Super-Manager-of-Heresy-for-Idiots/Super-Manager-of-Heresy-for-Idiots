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
     * Возвращает браузируемый список существующего контента автора заданного типа, доступного для
     * прикрепления к пакету (замена ручному вводу UUID в UI).
     * @param id идентификатор целевого пакета
     * @param type тип контента
     * @param auth аутентификация
     * @return список кандидатов на прикрепление
     */
    @GetMapping("/my/{id}/attachable")
    @Operation(summary = "List the author's existing content of a type that can be attached to the package")
    public CompletableFuture<ResponseEntity<ApiResponse<List<AttachableContentResponse>>>> listAttachable(
            @PathVariable UUID id, @RequestParam String type, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<AttachableContentResponse> data = authoringService.listAttachableContent(id, type, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data));
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

    /**
     * Создаёт homebrew-предысторию в пакете (P2-3).
     * @param packageId идентификатор пакета
     * @param request тело предыстории
     * @param auth аутентификация автора
     * @return обновлённая детальная модель пакета
     */
    @PostMapping("/my/{packageId}/content/backgrounds")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> createPackageBackground(
            @PathVariable UUID packageId, @Valid @RequestBody CreateBackgroundRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewDetailResponse data = authoringService.createPackageBackground(packageId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(data, "Предыстория добавлена в пакет"));
        }, controllerTaskExecutor);
    }

    /**
     * Создаёт homebrew-ресурс в пакете (P2-3) — механизм Ярость/Ки (custom_resource_types).
     * @param packageId идентификатор пакета
     * @param request тело ресурса
     * @param auth аутентификация автора
     * @return обновлённая детальная модель пакета
     */
    @PostMapping("/my/{packageId}/content/custom-resources")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> createPackageCustomResourceType(
            @PathVariable UUID packageId, @Valid @RequestBody CreateCustomResourceTypeRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            HomebrewDetailResponse data = authoringService.createPackageCustomResourceType(packageId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(data, "Ресурс добавлен в пакет"));
        }, controllerTaskExecutor);
    }

    // === P1-6: правка/удаление сущностей контента пакета ===

    @PutMapping("/my/{packageId}/content/item-types/{itemTypeId}")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> updatePackageItemType(
            @PathVariable UUID packageId, @PathVariable UUID itemTypeId,
            @Valid @RequestBody CreateItemTypeRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> ResponseEntity.ok(ApiResponse.ok(
                authoringService.updatePackageItemType(packageId, itemTypeId, request, auth.getName()),
                "Тип предмета обновлён")), controllerTaskExecutor);
    }

    @PutMapping("/my/{packageId}/content/skills/{skillId}")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> updatePackageSkill(
            @PathVariable UUID packageId, @PathVariable UUID skillId,
            @Valid @RequestBody CreateSkillRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> ResponseEntity.ok(ApiResponse.ok(
                authoringService.updatePackageSkill(packageId, skillId, request, auth.getName()),
                "Умение обновлено")), controllerTaskExecutor);
    }

    @PutMapping("/my/{packageId}/content/feats/{featId}")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> updatePackageFeat(
            @PathVariable UUID packageId, @PathVariable UUID featId,
            @Valid @RequestBody CreateFeatRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> ResponseEntity.ok(ApiResponse.ok(
                authoringService.updatePackageFeat(packageId, featId, request, auth.getName()),
                "Черта обновлена")), controllerTaskExecutor);
    }

    @PutMapping("/my/{packageId}/content/buffs-debuffs/{buffDebuffId}")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> updatePackageBuffDebuff(
            @PathVariable UUID packageId, @PathVariable UUID buffDebuffId,
            @Valid @RequestBody CreateBuffDebuffRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> ResponseEntity.ok(ApiResponse.ok(
                authoringService.updatePackageBuffDebuff(packageId, buffDebuffId, request, auth.getName()),
                "Бафф/дебафф обновлён")), controllerTaskExecutor);
    }

    @DeleteMapping("/my/{packageId}/content/item-types/{entityId}")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> deletePackageItemType(
            @PathVariable UUID packageId, @PathVariable UUID entityId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> ResponseEntity.ok(ApiResponse.ok(
                authoringService.deletePackageContentEntity(packageId, "ITEM_TYPE", entityId, auth.getName()),
                "Тип предмета удалён")), controllerTaskExecutor);
    }

    @DeleteMapping("/my/{packageId}/content/skills/{entityId}")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> deletePackageSkill(
            @PathVariable UUID packageId, @PathVariable UUID entityId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> ResponseEntity.ok(ApiResponse.ok(
                authoringService.deletePackageContentEntity(packageId, "SKILL", entityId, auth.getName()),
                "Умение удалено")), controllerTaskExecutor);
    }

    @DeleteMapping("/my/{packageId}/content/feats/{entityId}")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> deletePackageFeat(
            @PathVariable UUID packageId, @PathVariable UUID entityId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> ResponseEntity.ok(ApiResponse.ok(
                authoringService.deletePackageContentEntity(packageId, "FEAT", entityId, auth.getName()),
                "Черта удалена")), controllerTaskExecutor);
    }

    @DeleteMapping("/my/{packageId}/content/buffs-debuffs/{entityId}")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewDetailResponse>>> deletePackageBuffDebuff(
            @PathVariable UUID packageId, @PathVariable UUID entityId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> ResponseEntity.ok(ApiResponse.ok(
                authoringService.deletePackageContentEntity(packageId, "BUFF_DEBUFF", entityId, auth.getName()),
                "Бафф/дебафф удалён")), controllerTaskExecutor);
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
     * Пожаловаться на опубликованный homebrew-пакет (P2-6, пост-модерация).
     * @param id идентификатор пакета
     * @param request причина жалобы
     * @param auth аутентификация
     * @return подтверждение
     */
    @PostMapping("/marketplace/{id}/report")
    @Operation(summary = "Report a published homebrew package for moderation")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> reportPackage(
            @PathVariable UUID id,
            @Valid @RequestBody ReportHomebrewRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            marketplaceService.reportPackage(id, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(null, "Жалоба отправлена"));
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

    // NB: POST/DELETE /library/{packageId} («в библиотеку без установки») удалены при аудите эндпоинтов —
    // потребителей на FE не было (install уже добавляет в библиотеку). См. docs/HB_EP_AUDIT.md.
}
