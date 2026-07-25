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

/**
 * Класс LocationController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequestMapping("/api/campaigns/{campaignId}/locations")
@RequiredArgsConstructor
@Tag(name = "Locations", description = "Campaign location management")
public class LocationController {

    private final LocationService locationService;
    private final com.dnd.app.service.PresenceService presenceService;
    private final com.dnd.app.service.LocationMapService locationMapService;
    private final Executor controllerTaskExecutor;

    /**
     * Создает результат операции "create location" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Возвращает список для операции "list locations" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping
    @Operation(summary = "List locations")
    public CompletableFuture<ResponseEntity<ApiResponse<List<LocationResponse>>>> listLocations(
            @PathVariable UUID campaignId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<LocationResponse> locations = locationService.listLocations(campaignId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(locations));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get location" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param locationId идентификатор location, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Обновляет результат операции "update location" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param locationId идентификатор location, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Удаляет результат операции "delete location" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param locationId идентификатор location, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Преобразует данные операции "toggle visibility" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param locationId идентификатор location, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Помещает персонажа в локацию (WORLD_PLAN Этап 1: присутствие в мире).
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param locationId идентификатор location, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/{locationId}/enter")
    @Operation(summary = "Move a character into this location (owner or GM)")
    public CompletableFuture<ResponseEntity<ApiResponse<LocationRefResponse>>> enterLocation(
            @PathVariable UUID campaignId,
            @PathVariable UUID locationId,
            @Valid @RequestBody LocationPresenceRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            LocationRefResponse response = presenceService.enterLocation(
                    campaignId, locationId, request.getCharacterId(), auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Location entered"));
        }, controllerTaskExecutor);
    }

    /**
     * Убирает персонажа из локации (WORLD_PLAN Этап 1: присутствие в мире).
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param locationId идентификатор location, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/{locationId}/leave")
    @Operation(summary = "Move a character out of this location (owner or GM)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> leaveLocation(
            @PathVariable UUID campaignId,
            @PathVariable UUID locationId,
            @Valid @RequestBody LocationPresenceRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            presenceService.leaveLocation(campaignId, locationId, request.getCharacterId(), auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Location left"));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает обитателей локации (WORLD_PLAN Этап 1: присутствие в мире).
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param locationId идентификатор location, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/{locationId}/occupants")
    @Operation(summary = "List characters and NPCs currently in this location")
    public CompletableFuture<ResponseEntity<ApiResponse<LocationOccupantsResponse>>> getOccupants(
            @PathVariable UUID campaignId,
            @PathVariable UUID locationId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            LocationOccupantsResponse response = presenceService.getOccupants(campaignId, locationId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает карты, привязанные к локации (WORLD_PLAN Этап 4).
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param locationId идентификатор location, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/{locationId}/maps")
    @Operation(summary = "List map-service maps attached to this location")
    public CompletableFuture<ResponseEntity<ApiResponse<List<LocationMapResponse>>>> listLocationMaps(
            @PathVariable UUID campaignId,
            @PathVariable UUID locationId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<LocationMapResponse> response = locationMapService.listMaps(campaignId, locationId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response));
        }, controllerTaskExecutor);
    }

    /**
     * Привязывает карту map-service к локации (WORLD_PLAN Этап 4).
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param locationId идентификатор location, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/{locationId}/maps")
    @Operation(summary = "Attach a map-service map to this location (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<LocationMapResponse>>> attachLocationMap(
            @PathVariable UUID campaignId,
            @PathVariable UUID locationId,
            @Valid @RequestBody AttachLocationMapRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            LocationMapResponse response = locationMapService.attachMap(campaignId, locationId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Map attached"));
        }, controllerTaskExecutor);
    }

    /**
     * Отвязывает карту от локации (WORLD_PLAN Этап 4).
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param locationId идентификатор location, используемый для выбора нужного бизнес-объекта
     * @param linkId идентификатор привязки, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/{locationId}/maps/{linkId}")
    @Operation(summary = "Detach a map from this location (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> detachLocationMap(
            @PathVariable UUID campaignId,
            @PathVariable UUID locationId,
            @PathVariable UUID linkId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            locationMapService.detachMap(campaignId, locationId, linkId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Map detached"));
        }, controllerTaskExecutor);
    }
}
