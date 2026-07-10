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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Класс SharedStorageController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequestMapping("/api/campaigns/{campaignId}/shared-storage")
@RequiredArgsConstructor
@Tag(name = "Shared Storage", description = "Campaign shared storage management")
public class SharedStorageController {

    private final SharedStorageService sharedStorageService;
    private final Executor controllerTaskExecutor;

    /**
     * Создает результат операции "create storage" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping
    @Operation(summary = "Create shared storage container (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<SharedStorageResponse>>> createStorage(
            @PathVariable UUID campaignId,
            @Valid @RequestBody CreateSharedStorageRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            SharedStorageResponse response = sharedStorageService.createStorage(campaignId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Storage created"));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает список для операции "list storages" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping
    @Operation(summary = "List shared storage containers")
    public CompletableFuture<ResponseEntity<ApiResponse<List<SharedStorageResponse>>>> listStorages(
            @PathVariable UUID campaignId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<SharedStorageResponse> storages = sharedStorageService.listStorages(campaignId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(storages));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get storage" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param storageId идентификатор storage, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/{storageId}")
    @Operation(summary = "Get shared storage with items")
    public CompletableFuture<ResponseEntity<ApiResponse<SharedStorageResponse>>> getStorage(
            @PathVariable UUID campaignId,
            @PathVariable UUID storageId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            SharedStorageResponse response = sharedStorageService.getStorage(storageId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response));
        }, controllerTaskExecutor);
    }

    /**
     * Удаляет результат операции "delete storage" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param storageId идентификатор storage, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/{storageId}")
    @Operation(summary = "Delete shared storage (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteStorage(
            @PathVariable UUID campaignId,
            @PathVariable UUID storageId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            sharedStorageService.deleteStorage(storageId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Storage deleted"));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "deposit item" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param storageId идентификатор storage, используемый для выбора нужного бизнес-объекта
     * @param instanceId идентификатор instance, используемый для выбора нужного бизнес-объекта
     * @param quantity входящее значение quantity, используемое бизнес-сценарием
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/{storageId}/items/{instanceId}/deposit")
    @Operation(summary = "Deposit item into shared storage")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> depositItem(
            @PathVariable UUID campaignId,
            @PathVariable UUID storageId,
            @PathVariable UUID instanceId,
            @RequestParam(required = false) Integer quantity, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            sharedStorageService.addItemToStorage(storageId, instanceId, quantity, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Item deposited"));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "take item" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param storageId идентификатор storage, используемый для выбора нужного бизнес-объекта
     * @param instanceId идентификатор instance, используемый для выбора нужного бизнес-объекта
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param quantity входящее значение quantity, используемое бизнес-сценарием
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/{storageId}/items/{instanceId}/take/{characterId}")
    @Operation(summary = "Take item from shared storage")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> takeItem(
            @PathVariable UUID campaignId,
            @PathVariable UUID storageId,
            @PathVariable UUID instanceId,
            @PathVariable UUID characterId,
            @RequestParam(required = false) Integer quantity, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            sharedStorageService.takeItemFromStorage(storageId, instanceId, characterId, quantity, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Item taken"));
        }, controllerTaskExecutor);
    }
}
