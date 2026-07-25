package com.dnd.app.controller;

import com.dnd.app.dto.request.PutNpcDialogueRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.NpcDialogueResponse;
import com.dnd.app.service.NpcDialogueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Класс NpcDialogueController описывает REST-контроллер опционального диалога NPC
 * (WORLD_PLAN Этап 4). Мастер по желанию сохраняет/удаляет дерево реплик.
 */
@RestController
@RequestMapping("/api/campaigns/{campaignId}/npcs/{npcId}/dialogue")
@RequiredArgsConstructor
@Tag(name = "NPC Dialogue", description = "Optional GM-authored NPC dialogue tree")
public class NpcDialogueController {

    private final NpcDialogueService dialogueService;
    private final Executor controllerTaskExecutor;

    /**
     * Возвращает диалог NPC или пустое тело, если он не настроен.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping
    @Operation(summary = "Get an NPC's dialogue tree (null if not configured)")
    public CompletableFuture<ResponseEntity<ApiResponse<NpcDialogueResponse>>> getDialogue(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            NpcDialogueResponse data = dialogueService.getDialogue(campaignId, npcId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    /**
     * Сохраняет диалог NPC целиком (только мастер). Пустой список узлов удаляет диалог.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PutMapping
    @Operation(summary = "Create or replace an NPC's dialogue tree (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<NpcDialogueResponse>>> putDialogue(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId,
            @Valid @RequestBody PutNpcDialogueRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            NpcDialogueResponse data = dialogueService.putDialogue(campaignId, npcId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Dialogue saved"));
        }, controllerTaskExecutor);
    }

    /**
     * Удаляет диалог NPC (только мастер).
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping
    @Operation(summary = "Delete an NPC's dialogue tree (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteDialogue(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            dialogueService.deleteDialogue(campaignId, npcId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Dialogue deleted"));
        }, controllerTaskExecutor);
    }
}
