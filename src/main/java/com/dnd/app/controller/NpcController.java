package com.dnd.app.controller;

import com.dnd.app.dto.request.*;
import com.dnd.app.dto.response.*;
import com.dnd.app.service.NpcService;
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
 * Класс NpcController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequestMapping("/api/campaigns/{campaignId}/npcs")
@RequiredArgsConstructor
@Tag(name = "NPCs", description = "Campaign NPC management")
public class NpcController {

    private final NpcService npcService;
    private final Executor controllerTaskExecutor;

    /**
     * Создает результат операции "create npc" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping
    @Operation(summary = "Create NPC (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<NpcResponse>>> createNpc(
            @PathVariable UUID campaignId,
            @Valid @RequestBody CreateNpcRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            NpcResponse response = npcService.createNpc(campaignId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "NPC created"));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает список для операции "list npcs" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping
    @Operation(summary = "List NPCs")
    public CompletableFuture<ResponseEntity<ApiResponse<List<NpcResponse>>>> listNpcs(
            @PathVariable UUID campaignId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<NpcResponse> npcs = npcService.listNpcs(campaignId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(npcs));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get npc" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/{npcId}")
    @Operation(summary = "Get NPC details")
    public CompletableFuture<ResponseEntity<ApiResponse<NpcResponse>>> getNpc(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            NpcResponse response = npcService.getNpc(npcId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response));
        }, controllerTaskExecutor);
    }

    /**
     * Обновляет результат операции "update npc" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PutMapping("/{npcId}")
    @Operation(summary = "Update NPC (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<NpcResponse>>> updateNpc(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId,
            @Valid @RequestBody UpdateNpcRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            NpcResponse response = npcService.updateNpc(npcId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "NPC updated"));
        }, controllerTaskExecutor);
    }

    /**
     * Удаляет результат операции "delete npc" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/{npcId}")
    @Operation(summary = "Delete NPC (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteNpc(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            npcService.deleteNpc(npcId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "NPC deleted"));
        }, controllerTaskExecutor);
    }

    /**
     * Преобразует данные операции "toggle visibility" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/{npcId}/toggle-visibility")
    @Operation(summary = "Toggle NPC visibility (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<NpcResponse>>> toggleVisibility(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            NpcResponse response = npcService.toggleVisibility(npcId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Visibility toggled"));
        }, controllerTaskExecutor);
    }

    /**
     * Добавляет результат операции "add note" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/{npcId}/notes")
    @Operation(summary = "Add note to NPC")
    public CompletableFuture<ResponseEntity<ApiResponse<NoteResponse>>> addNote(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId,
            @Valid @RequestBody CreateNoteRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            NoteResponse response = npcService.addNote(npcId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Note added"));
        }, controllerTaskExecutor);
    }

    /**
     * Обновляет результат операции "update note" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param noteId идентификатор note, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PutMapping("/{npcId}/notes/{noteId}")
    @Operation(summary = "Update note")
    public CompletableFuture<ResponseEntity<ApiResponse<NoteResponse>>> updateNote(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId,
            @PathVariable UUID noteId,
            @Valid @RequestBody UpdateNoteRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            NoteResponse response = npcService.updateNote(noteId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Note updated"));
        }, controllerTaskExecutor);
    }

    /**
     * Удаляет результат операции "delete note" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param noteId идентификатор note, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/{npcId}/notes/{noteId}")
    @Operation(summary = "Delete note")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteNote(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId,
            @PathVariable UUID noteId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            npcService.deleteNote(noteId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Note deleted"));
        }, controllerTaskExecutor);
    }
}
