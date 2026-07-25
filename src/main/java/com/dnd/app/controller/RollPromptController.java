package com.dnd.app.controller;

import com.dnd.app.domain.enums.RollPromptStatus;
import com.dnd.app.dto.request.CreateRollPromptRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.RollPromptResponse;
import com.dnd.app.service.RollPromptService;
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
 * Класс RollPromptController описывает REST-контроллер запрошенных мастером проверок
 * (ROLL_PROMPT): мастер создаёт/отменяет запросы, игрок видит свои и бросает.
 */
@RestController
@RequestMapping("/api/campaigns/{campaignId}/roll-prompts")
@RequiredArgsConstructor
@Tag(name = "Roll Prompts", description = "GM-initiated checks: player roll windows")
public class RollPromptController {

    private final RollPromptService rollPromptService;
    private final Executor controllerTaskExecutor;

    /**
     * Мастер запрашивает проверку у персонажей (ROLL_PROMPT).
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping
    @Operation(summary = "Request a check from characters (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<RollPromptResponse>>>> createPrompts(
            @PathVariable UUID campaignId,
            @Valid @RequestBody CreateRollPromptRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<RollPromptResponse> response = rollPromptService.createPrompts(campaignId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Roll prompts created"));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает запросы проверок (мастер — все, игрок — своих персонажей).
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param status фильтр по статусу (опционально)
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping
    @Operation(summary = "List roll prompts (GM: all, player: own characters)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<RollPromptResponse>>>> listPrompts(
            @PathVariable UUID campaignId,
            @RequestParam(required = false) RollPromptStatus status, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<RollPromptResponse> response = rollPromptService.listPrompts(campaignId, status, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response));
        }, controllerTaskExecutor);
    }

    /**
     * Игрок совершает бросок по запросу (d20 исполняется на сервере).
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param promptId идентификатор prompt, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/{promptId}/roll")
    @Operation(summary = "Make the roll (character owner; server-side d20)")
    public CompletableFuture<ResponseEntity<ApiResponse<RollPromptResponse>>> roll(
            @PathVariable UUID campaignId,
            @PathVariable UUID promptId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            RollPromptResponse response = rollPromptService.roll(campaignId, promptId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Rolled"));
        }, controllerTaskExecutor);
    }

    /**
     * Мастер отменяет ожидающий запрос проверки.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param promptId идентификатор prompt, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/{promptId}/cancel")
    @Operation(summary = "Cancel a pending roll prompt (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> cancel(
            @PathVariable UUID campaignId,
            @PathVariable UUID promptId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            rollPromptService.cancel(campaignId, promptId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Roll prompt cancelled"));
        }, controllerTaskExecutor);
    }
}
