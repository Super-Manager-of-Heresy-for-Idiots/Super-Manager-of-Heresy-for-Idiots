package com.dnd.app.controller;

import com.dnd.app.dto.request.*;
import com.dnd.app.dto.response.*;
import com.dnd.app.service.CampaignBlueprintMarketplaceService;
import com.dnd.app.service.CampaignBlueprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Класс CampaignBlueprintController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequestMapping("/api/campaign-blueprints")
@RequiredArgsConstructor
@Tag(name = "Campaign Blueprints", description = "Шаблоны кампаний: авторинг, каталог, форк и инстанцирование")
public class CampaignBlueprintController {

    private final CampaignBlueprintService blueprintService;
    private final CampaignBlueprintMarketplaceService marketplaceService;
    private final Executor controllerTaskExecutor;

    // ============================ Authoring ============================

    /**
     * Создает результат операции "create" в рамках бизнес-логики API.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/my")
    @Operation(summary = "Создать шаблон кампании (мастер игры/админ)")
    public CompletableFuture<ResponseEntity<ApiResponse<CampaignBlueprintDetailResponse>>> create(
            @Valid @RequestBody CreateCampaignBlueprintRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampaignBlueprintDetailResponse response = blueprintService.createBlueprint(request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(response, "Шаблон кампании создан"));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает список для операции "list mine" в рамках бизнес-логики API.
     * @param pageable параметры постраничной выдачи для бизнес-списка
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/my")
    @Operation(summary = "Список моих шаблонов кампаний (постранично)")
    public CompletableFuture<ResponseEntity<ApiResponse<Page<CampaignBlueprintResponse>>>> listMine(
            Pageable pageable, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            Page<CampaignBlueprintResponse> page = blueprintService.listMyBlueprints(auth.getName(), pageable);
            return ResponseEntity.ok(ApiResponse.ok(page));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get mine" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/my/{id}")
    @Operation(summary = "Получить мой шаблон кампании")
    public CompletableFuture<ResponseEntity<ApiResponse<CampaignBlueprintDetailResponse>>> getMine(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampaignBlueprintDetailResponse response = blueprintService.getMyBlueprint(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response));
        }, controllerTaskExecutor);
    }

    /**
     * Обновляет результат операции "update" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PutMapping("/my/{id}")
    @Operation(summary = "Обновить шаблон кампании (только черновик)")
    public CompletableFuture<ResponseEntity<ApiResponse<CampaignBlueprintDetailResponse>>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCampaignBlueprintRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampaignBlueprintDetailResponse response = blueprintService.updateBlueprint(id, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Шаблон обновлён"));
        }, controllerTaskExecutor);
    }

    /**
     * Удаляет результат операции "delete" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/my/{id}")
    @Operation(summary = "Удалить шаблон кампании (мягкое удаление)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> delete(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            blueprintService.softDeleteBlueprint(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Шаблон удалён"));
        }, controllerTaskExecutor);
    }

    /**
     * Публикует событие операции "publish" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/my/{id}/publish")
    @Operation(summary = "Опубликовать шаблон кампании")
    public CompletableFuture<ResponseEntity<ApiResponse<CampaignBlueprintDetailResponse>>> publish(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampaignBlueprintDetailResponse response = blueprintService.publish(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Шаблон опубликован"));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "unpublish" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/my/{id}/unpublish")
    @Operation(summary = "Снять шаблон с публикации")
    public CompletableFuture<ResponseEntity<ApiResponse<CampaignBlueprintDetailResponse>>> unpublish(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampaignBlueprintDetailResponse response = blueprintService.unpublish(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Шаблон снят с публикации"));
        }, controllerTaskExecutor);
    }

    // ============================ NPCs ============================

    /**
     * Создает результат операции "create npc" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/my/{id}/npcs")
    @Operation(summary = "Добавить NPC в шаблон")
    public CompletableFuture<ResponseEntity<ApiResponse<NpcResponse>>> createNpc(
            @PathVariable UUID id, @Valid @RequestBody CreateNpcRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            NpcResponse response = blueprintService.createNpc(id, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "NPC добавлен"));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает список для операции "list npcs" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/my/{id}/npcs")
    @Operation(summary = "Список NPC шаблона")
    public CompletableFuture<ResponseEntity<ApiResponse<List<NpcResponse>>>> listNpcs(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(blueprintService.listNpcs(id, auth.getName()))),
                controllerTaskExecutor);
    }

    /**
     * Обновляет результат операции "update npc" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PutMapping("/my/{id}/npcs/{npcId}")
    @Operation(summary = "Обновить NPC шаблона")
    public CompletableFuture<ResponseEntity<ApiResponse<NpcResponse>>> updateNpc(
            @PathVariable UUID id, @PathVariable UUID npcId,
            @Valid @RequestBody UpdateNpcRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            NpcResponse response = blueprintService.updateNpc(id, npcId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "NPC обновлён"));
        }, controllerTaskExecutor);
    }

    /**
     * Удаляет результат операции "delete npc" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/my/{id}/npcs/{npcId}")
    @Operation(summary = "Удалить NPC шаблона")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteNpc(
            @PathVariable UUID id, @PathVariable UUID npcId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            blueprintService.deleteNpc(id, npcId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "NPC удалён"));
        }, controllerTaskExecutor);
    }

    // ============================ Quests ============================

    /**
     * Создает результат операции "create quest" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/my/{id}/quests")
    @Operation(summary = "Добавить квест в шаблон")
    public CompletableFuture<ResponseEntity<ApiResponse<QuestResponse>>> createQuest(
            @PathVariable UUID id, @Valid @RequestBody CreateQuestRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            QuestResponse response = blueprintService.createQuest(id, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Квест добавлен"));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает список для операции "list quests" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/my/{id}/quests")
    @Operation(summary = "Список квестов шаблона")
    public CompletableFuture<ResponseEntity<ApiResponse<List<QuestResponse>>>> listQuests(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(blueprintService.listQuests(id, auth.getName()))),
                controllerTaskExecutor);
    }

    /**
     * Обновляет результат операции "update quest" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PutMapping("/my/{id}/quests/{questId}")
    @Operation(summary = "Обновить квест шаблона")
    public CompletableFuture<ResponseEntity<ApiResponse<QuestResponse>>> updateQuest(
            @PathVariable UUID id, @PathVariable UUID questId,
            @Valid @RequestBody UpdateQuestRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            QuestResponse response = blueprintService.updateQuest(id, questId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Квест обновлён"));
        }, controllerTaskExecutor);
    }

    /**
     * Удаляет результат операции "delete quest" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/my/{id}/quests/{questId}")
    @Operation(summary = "Удалить квест шаблона")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteQuest(
            @PathVariable UUID id, @PathVariable UUID questId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            blueprintService.deleteQuest(id, questId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Квест удалён"));
        }, controllerTaskExecutor);
    }

    /**
     * Добавляет результат операции "add reward" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/my/{id}/quests/{questId}/rewards")
    @Operation(summary = "Добавить награду к квесту шаблона")
    public CompletableFuture<ResponseEntity<ApiResponse<QuestRewardResponse>>> addReward(
            @PathVariable UUID id, @PathVariable UUID questId,
            @Valid @RequestBody CreateQuestRewardRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            QuestRewardResponse response = blueprintService.addReward(id, questId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Награда добавлена"));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает список для операции "list rewards" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/my/{id}/quests/{questId}/rewards")
    @Operation(summary = "Список наград квеста шаблона")
    public CompletableFuture<ResponseEntity<ApiResponse<List<QuestRewardResponse>>>> listRewards(
            @PathVariable UUID id, @PathVariable UUID questId, Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(blueprintService.listRewards(id, questId, auth.getName()))),
                controllerTaskExecutor);
    }

    /**
     * Удаляет результат операции "delete reward" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param rewardId идентификатор reward, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/my/{id}/quests/{questId}/rewards/{rewardId}")
    @Operation(summary = "Удалить награду квеста шаблона")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteReward(
            @PathVariable UUID id, @PathVariable UUID questId, @PathVariable UUID rewardId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            blueprintService.deleteReward(id, questId, rewardId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Награда удалена"));
        }, controllerTaskExecutor);
    }

    // ============================ Locations ============================

    /**
     * Создает результат операции "create location" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/my/{id}/locations")
    @Operation(summary = "Добавить локацию в шаблон")
    public CompletableFuture<ResponseEntity<ApiResponse<LocationResponse>>> createLocation(
            @PathVariable UUID id, @Valid @RequestBody CreateLocationRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            LocationResponse response = blueprintService.createLocation(id, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Локация добавлена"));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает список для операции "list locations" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/my/{id}/locations")
    @Operation(summary = "Список локаций шаблона")
    public CompletableFuture<ResponseEntity<ApiResponse<List<LocationResponse>>>> listLocations(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(blueprintService.listLocations(id, auth.getName()))),
                controllerTaskExecutor);
    }

    /**
     * Обновляет результат операции "update location" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param locationId идентификатор location, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PutMapping("/my/{id}/locations/{locationId}")
    @Operation(summary = "Обновить локацию шаблона")
    public CompletableFuture<ResponseEntity<ApiResponse<LocationResponse>>> updateLocation(
            @PathVariable UUID id, @PathVariable UUID locationId,
            @Valid @RequestBody UpdateLocationRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            LocationResponse response = blueprintService.updateLocation(id, locationId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Локация обновлена"));
        }, controllerTaskExecutor);
    }

    /**
     * Удаляет результат операции "delete location" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param locationId идентификатор location, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/my/{id}/locations/{locationId}")
    @Operation(summary = "Удалить локацию шаблона")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteLocation(
            @PathVariable UUID id, @PathVariable UUID locationId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            blueprintService.deleteLocation(id, locationId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Локация удалена"));
        }, controllerTaskExecutor);
    }

    // ============================ Homebrew ============================

    /**
     * Выполняет операции "attach homebrew" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/my/{id}/homebrew")
    @Operation(summary = "Подключить homebrew-пакет к шаблону")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> attachHomebrew(
            @PathVariable UUID id, @Valid @RequestBody ActivateHomebrewRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            blueprintService.attachHomebrew(id, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(null, "Homebrew-пакет подключён к шаблону"));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "detach homebrew" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/my/{id}/homebrew/{packageId}")
    @Operation(summary = "Отключить homebrew-пакет от шаблона")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> detachHomebrew(
            @PathVariable UUID id, @PathVariable UUID packageId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            blueprintService.detachHomebrew(id, packageId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Homebrew-пакет отключён от шаблона"));
        }, controllerTaskExecutor);
    }

    // ============================ Pre-built characters ============================

    /**
     * Выполняет операции "link character" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/my/{id}/characters/{characterId}")
    @Operation(summary = "Привязать персонажа-пребилд к шаблону")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> linkCharacter(
            @PathVariable UUID id, @PathVariable UUID characterId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            blueprintService.linkCharacter(id, characterId, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(null, "Персонаж привязан к шаблону"));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "unlink character" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/my/{id}/characters/{characterId}")
    @Operation(summary = "Отвязать персонажа-пребилд от шаблона")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> unlinkCharacter(
            @PathVariable UUID id, @PathVariable UUID characterId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            blueprintService.unlinkCharacter(id, characterId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Персонаж отвязан от шаблона"));
        }, controllerTaskExecutor);
    }

    // ============================ Marketplace ============================

    /**
     * Выполняет операции "browse marketplace" в рамках бизнес-логики API.
     * @param search входящее значение search, используемое бизнес-сценарием
     * @param universe входящее значение universe, используемое бизнес-сценарием
     * @param sort входящее значение sort, используемое бизнес-сценарием
     * @param page входящее значение page, используемое бизнес-сценарием
     * @param size входящее значение size, используемое бизнес-сценарием
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/marketplace")
    @Operation(summary = "Каталог опубликованных шаблонов (доступно всем)")
    public CompletableFuture<ResponseEntity<ApiResponse<Page<CampaignBlueprintResponse>>>> browseMarketplace(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, name = "universe") String universe,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            Page<CampaignBlueprintResponse> result =
                    marketplaceService.browseMarketplace(search, universe, sort, page, size, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(result));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get marketplace blueprint" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/marketplace/{id}")
    @Operation(summary = "Детали опубликованного шаблона (доступно всем)")
    public CompletableFuture<ResponseEntity<ApiResponse<CampaignBlueprintDetailResponse>>> getMarketplaceBlueprint(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampaignBlueprintDetailResponse response = marketplaceService.getMarketplaceBlueprint(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "fork" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/marketplace/{id}/fork")
    @Operation(summary = "Форкнуть опубликованный шаблон (мастер игры/админ)")
    public CompletableFuture<ResponseEntity<ApiResponse<CampaignBlueprintDetailResponse>>> fork(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampaignBlueprintDetailResponse response = marketplaceService.fork(id, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(response, "Создан форк шаблона"));
        }, controllerTaskExecutor);
    }

    // ============================ Instantiate ============================

    /**
     * Выполняет операции "instantiate" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/{id}/instantiate")
    @Operation(summary = "Создать кампанию из шаблона")
    public CompletableFuture<ResponseEntity<ApiResponse<CampaignResponse>>> instantiate(
            @PathVariable UUID id, @Valid @RequestBody InstantiateBlueprintRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampaignResponse response = marketplaceService.instantiate(id, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(response, "Кампания создана из шаблона"));
        }, controllerTaskExecutor);
    }
}
