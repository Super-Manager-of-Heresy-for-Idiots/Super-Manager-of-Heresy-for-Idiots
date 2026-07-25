package com.dnd.app.controller;

import com.dnd.app.dto.request.CreateMapTransitionRequest;
import com.dnd.app.dto.request.TraverseTransitionRequest;
import com.dnd.app.dto.request.UpdateMapTransitionRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.MapTransitionResponse;
import com.dnd.app.dto.response.TraverseResultResponse;
import com.dnd.app.service.MapTransitionService;
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
 * Класс MapTransitionController описывает REST-контроллер переходов между картами
 * через ключевые клетки (WORLD_PLAN Этап 5): CRUD для ГМ и игровой traverse.
 */
@RestController
@RequestMapping("/api/campaigns/{campaignId}/map-transitions")
@RequiredArgsConstructor
@Tag(name = "Map Transitions", description = "Key-cell transitions between maps and world travel")
public class MapTransitionController {

    private final MapTransitionService mapTransitionService;
    private final Executor controllerTaskExecutor;

    /**
     * Возвращает переходы кампании (фильтр по исходной карте — опционально).
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param mapId идентификатор исходной карты map-service (опционально)
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping
    @Operation(summary = "List map transitions (players see only enabled ones)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<MapTransitionResponse>>>> listTransitions(
            @PathVariable UUID campaignId,
            @RequestParam(required = false) UUID mapId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<MapTransitionResponse> response = mapTransitionService.listTransitions(campaignId, mapId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response));
        }, controllerTaskExecutor);
    }

    /**
     * Создаёт переход между картами.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping
    @Operation(summary = "Create a map transition (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<MapTransitionResponse>>> createTransition(
            @PathVariable UUID campaignId,
            @Valid @RequestBody CreateMapTransitionRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            MapTransitionResponse response = mapTransitionService.createTransition(campaignId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Transition created"));
        }, controllerTaskExecutor);
    }

    /**
     * Обновляет переход (в т.ч. запирает/отпирает через enabled).
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param transitionId идентификатор transition, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PutMapping("/{transitionId}")
    @Operation(summary = "Update a map transition (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<MapTransitionResponse>>> updateTransition(
            @PathVariable UUID campaignId,
            @PathVariable UUID transitionId,
            @Valid @RequestBody UpdateMapTransitionRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            MapTransitionResponse response = mapTransitionService.updateTransition(
                    campaignId, transitionId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Transition updated"));
        }, controllerTaskExecutor);
    }

    /**
     * Удаляет переход.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param transitionId идентификатор transition, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/{transitionId}")
    @Operation(summary = "Delete a map transition (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteTransition(
            @PathVariable UUID campaignId,
            @PathVariable UUID transitionId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            mapTransitionService.deleteTransition(campaignId, transitionId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Transition deleted"));
        }, controllerTaskExecutor);
    }

    /**
     * Проход персонажа через переход: смена локации, перенос токена, выход из боя.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param transitionId идентификатор transition, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/{transitionId}/traverse")
    @Operation(summary = "Traverse a transition with a character (world travel + token relocation)")
    public CompletableFuture<ResponseEntity<ApiResponse<TraverseResultResponse>>> traverse(
            @PathVariable UUID campaignId,
            @PathVariable UUID transitionId,
            @Valid @RequestBody TraverseTransitionRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            TraverseResultResponse response = mapTransitionService.traverse(
                    campaignId, transitionId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Transition traversed"));
        }, controllerTaskExecutor);
    }
}
