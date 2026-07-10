package com.dnd.app.controller;

import com.dnd.app.dto.request.*;
import com.dnd.app.dto.response.*;
import com.dnd.app.service.GmSessionNoteService;
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
 * Класс GmSessionNoteController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequestMapping("/api/campaigns/{campaignId}/gm-notes")
@RequiredArgsConstructor
@Tag(name = "GM Session Notes", description = "GM-only session notes")
public class GmSessionNoteController {

    private final GmSessionNoteService gmSessionNoteService;
    private final Executor controllerTaskExecutor;

    /**
     * Создает результат операции "create note" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping
    @Operation(summary = "Create GM session note")
    public CompletableFuture<ResponseEntity<ApiResponse<GmSessionNoteResponse>>> createNote(
            @PathVariable UUID campaignId,
            @Valid @RequestBody CreateGmNoteRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            GmSessionNoteResponse response = gmSessionNoteService.createNote(campaignId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Note created"));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает список для операции "list notes" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping
    @Operation(summary = "List GM session notes")
    public CompletableFuture<ResponseEntity<ApiResponse<List<GmSessionNoteResponse>>>> listNotes(
            @PathVariable UUID campaignId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<GmSessionNoteResponse> notes = gmSessionNoteService.listNotes(campaignId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(notes));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get note" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param noteId идентификатор note, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/{noteId}")
    @Operation(summary = "Get GM session note")
    public CompletableFuture<ResponseEntity<ApiResponse<GmSessionNoteResponse>>> getNote(
            @PathVariable UUID campaignId,
            @PathVariable UUID noteId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            GmSessionNoteResponse response = gmSessionNoteService.getNote(noteId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response));
        }, controllerTaskExecutor);
    }

    /**
     * Обновляет результат операции "update note" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param noteId идентификатор note, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PutMapping("/{noteId}")
    @Operation(summary = "Update GM session note")
    public CompletableFuture<ResponseEntity<ApiResponse<GmSessionNoteResponse>>> updateNote(
            @PathVariable UUID campaignId,
            @PathVariable UUID noteId,
            @Valid @RequestBody UpdateGmNoteRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            GmSessionNoteResponse response = gmSessionNoteService.updateNote(noteId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Note updated"));
        }, controllerTaskExecutor);
    }

    /**
     * Удаляет результат операции "delete note" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param noteId идентификатор note, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/{noteId}")
    @Operation(summary = "Delete GM session note")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteNote(
            @PathVariable UUID campaignId,
            @PathVariable UUID noteId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            gmSessionNoteService.deleteNote(noteId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Note deleted"));
        }, controllerTaskExecutor);
    }
}
