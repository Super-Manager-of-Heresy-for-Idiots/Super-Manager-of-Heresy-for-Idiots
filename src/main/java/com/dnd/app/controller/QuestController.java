package com.dnd.app.controller;

import com.dnd.app.dto.request.*;
import com.dnd.app.dto.response.*;
import com.dnd.app.service.QuestService;
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
 * Класс QuestController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequestMapping("/api/campaigns/{campaignId}/quests")
@RequiredArgsConstructor
@Tag(name = "Quests", description = "Campaign quest management")
public class QuestController {

    private final QuestService questService;
    private final Executor controllerTaskExecutor;

    /**
     * Создает результат операции "create quest" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping
    @Operation(summary = "Create quest (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<QuestResponse>>> createQuest(
            @PathVariable UUID campaignId,
            @Valid @RequestBody CreateQuestRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            QuestResponse response = questService.createQuest(campaignId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Quest created"));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает список для операции "list quests" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping
    @Operation(summary = "List quests")
    public CompletableFuture<ResponseEntity<ApiResponse<List<QuestResponse>>>> listQuests(
            @PathVariable UUID campaignId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<QuestResponse> quests = questService.listQuests(campaignId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(quests));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get quest" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/{questId}")
    @Operation(summary = "Get quest details")
    public CompletableFuture<ResponseEntity<ApiResponse<QuestResponse>>> getQuest(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            QuestResponse response = questService.getQuest(questId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response));
        }, controllerTaskExecutor);
    }

    /**
     * Обновляет результат операции "update quest" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PutMapping("/{questId}")
    @Operation(summary = "Update quest (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<QuestResponse>>> updateQuest(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId,
            @Valid @RequestBody UpdateQuestRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            QuestResponse response = questService.updateQuest(questId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Quest updated"));
        }, controllerTaskExecutor);
    }

    /**
     * Удаляет результат операции "delete quest" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/{questId}")
    @Operation(summary = "Delete quest (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteQuest(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            questService.deleteQuest(questId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Quest deleted"));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "complete quest" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/{questId}/complete")
    @Operation(summary = "Complete quest and grant its reward to a recipient character (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<QuestCompletionResponse>>> completeQuest(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId,
            @Valid @RequestBody com.dnd.app.dto.request.CompleteQuestRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            QuestCompletionResponse response = questService.completeQuest(questId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Quest completed and reward granted"));
        }, controllerTaskExecutor);
    }

    // --- Quest rewards ---

    /**
     * Возвращает список для операции "list rewards" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/{questId}/rewards")
    @Operation(summary = "List quest rewards")
    public CompletableFuture<ResponseEntity<ApiResponse<List<QuestRewardResponse>>>> listRewards(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<QuestRewardResponse> rewards = questService.listRewards(questId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(rewards));
        }, controllerTaskExecutor);
    }

    /**
     * Добавляет результат операции "add reward" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/{questId}/rewards")
    @Operation(summary = "Add reward to quest (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<QuestRewardResponse>>> addReward(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId,
            @Valid @RequestBody com.dnd.app.dto.request.CreateQuestRewardRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            QuestRewardResponse response = questService.addReward(questId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Reward added"));
        }, controllerTaskExecutor);
    }

    /**
     * Удаляет результат операции "delete reward" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param rewardId идентификатор reward, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/{questId}/rewards/{rewardId}")
    @Operation(summary = "Delete quest reward (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteReward(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId,
            @PathVariable UUID rewardId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            questService.deleteReward(rewardId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Reward deleted"));
        }, controllerTaskExecutor);
    }

    // --- Notes ---

    /**
     * Добавляет результат операции "add note" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/{questId}/notes")
    @Operation(summary = "Add note to quest")
    public CompletableFuture<ResponseEntity<ApiResponse<NoteResponse>>> addNote(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId,
            @Valid @RequestBody CreateNoteRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            NoteResponse response = questService.addNote(questId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Note added"));
        }, controllerTaskExecutor);
    }

    /**
     * Обновляет результат операции "update note" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param noteId идентификатор note, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PutMapping("/{questId}/notes/{noteId}")
    @Operation(summary = "Update quest note")
    public CompletableFuture<ResponseEntity<ApiResponse<NoteResponse>>> updateNote(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId,
            @PathVariable UUID noteId,
            @Valid @RequestBody UpdateNoteRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            NoteResponse response = questService.updateNote(noteId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Note updated"));
        }, controllerTaskExecutor);
    }

    /**
     * Удаляет результат операции "delete note" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param noteId идентификатор note, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/{questId}/notes/{noteId}")
    @Operation(summary = "Delete quest note")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteNote(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId,
            @PathVariable UUID noteId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            questService.deleteNote(noteId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Note deleted"));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "link npc" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/{questId}/npcs/{npcId}")
    @Operation(summary = "Link NPC to quest (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> linkNpc(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId,
            @PathVariable UUID npcId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            questService.linkNpc(questId, npcId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "NPC linked to quest"));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "unlink npc" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/{questId}/npcs/{npcId}")
    @Operation(summary = "Unlink NPC from quest (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> unlinkNpc(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId,
            @PathVariable UUID npcId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            questService.unlinkNpc(questId, npcId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "NPC unlinked from quest"));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "link location" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param locationId идентификатор location, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/{questId}/locations/{locationId}")
    @Operation(summary = "Link location to quest (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> linkLocation(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId,
            @PathVariable UUID locationId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            questService.linkLocation(questId, locationId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Location linked to quest"));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "unlink location" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param locationId идентификатор location, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/{questId}/locations/{locationId}")
    @Operation(summary = "Unlink location from quest (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> unlinkLocation(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId,
            @PathVariable UUID locationId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            questService.unlinkLocation(questId, locationId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Location unlinked from quest"));
        }, controllerTaskExecutor);
    }
}
