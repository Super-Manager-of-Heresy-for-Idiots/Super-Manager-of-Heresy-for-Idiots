package com.dnd.app.controller;

import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.CharacterQuestResponse;
import com.dnd.app.service.CharacterQuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Класс CharacterQuestController описывает REST-контроллер журнала квестов персонажа
 * (WORLD_PLAN Этап 2): просмотр журнала и отказ от взятого квеста. Взятие квеста —
 * на стороне NPC ({@code NpcController#acceptQuest}).
 */
@RestController
@RequestMapping("/api/campaigns/{campaignId}/characters/{characterId}/quests")
@RequiredArgsConstructor
@Tag(name = "Character Quests", description = "Per-character quest journal")
public class CharacterQuestController {

    private final CharacterQuestService characterQuestService;
    private final Executor controllerTaskExecutor;

    /**
     * Возвращает журнал квестов персонажа (владелец или ГМ).
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping
    @Operation(summary = "Character quest journal (owner or GM)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<CharacterQuestResponse>>>> getJournal(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<CharacterQuestResponse> response = characterQuestService.getJournal(campaignId, characterId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response));
        }, controllerTaskExecutor);
    }

    /**
     * Отказ персонажа от взятого квеста (WORLD_PLAN Этап 2).
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/{questId}/abandon")
    @Operation(summary = "Abandon an accepted quest (owner or GM)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> abandonQuest(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId,
            @PathVariable UUID questId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            characterQuestService.abandonQuest(campaignId, characterId, questId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Quest abandoned"));
        }, controllerTaskExecutor);
    }
}
