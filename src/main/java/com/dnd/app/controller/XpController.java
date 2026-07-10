package com.dnd.app.controller;

import com.dnd.app.dto.request.DistributeXpRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.service.XpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Класс XpController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequestMapping("/api/campaigns/{campaignId}/xp")
@RequiredArgsConstructor
@Tag(name = "XP Distribution", description = "Experience point distribution")
public class XpController {

    private final XpService xpService;
    private final Executor controllerTaskExecutor;

    /**
     * Выполняет операции "distribute xp" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping
    @Operation(summary = "Distribute XP to characters (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<Map<String, Object>>>> distributeXp(
            @PathVariable UUID campaignId,
            @Valid @RequestBody DistributeXpRequest request,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> result = xpService.distributeXp(campaignId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(result, "XP distributed"));
        }, controllerTaskExecutor);
    }
}
