package com.dnd.app.controller;

import com.dnd.app.domain.enums.DictionaryKind;
import com.dnd.app.dto.request.DictionaryEntryRequest;
import com.dnd.app.dto.request.MonsterRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.DictionaryEntryResponse;
import com.dnd.app.dto.response.MonsterResponse;
import com.dnd.app.dto.response.MonsterSummaryResponse;
import com.dnd.app.service.BestiaryDictionaryService;
import com.dnd.app.service.MonsterService;
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
 * Класс HomebrewBestiaryController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequestMapping("/api/homebrew/{packageId}/bestiary")
@RequiredArgsConstructor
@Tag(name = "Homebrew Bestiary", description = "Homebrew monsters and reference dictionaries (GAME_MASTER)")
public class HomebrewBestiaryController {

    private final BestiaryDictionaryService dictionaryService;
    private final MonsterService monsterService;
    private final Executor controllerTaskExecutor;

    // === Homebrew dictionaries ===

    /**
     * Возвращает список для операции "list dictionary" в рамках бизнес-логики API.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param kind входящее значение kind, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/dictionaries/{kind}")
    @Operation(summary = "List homebrew dictionary entries")
    public CompletableFuture<ResponseEntity<ApiResponse<List<DictionaryEntryResponse>>>> listDictionary(
            @PathVariable UUID packageId,
            @PathVariable String kind) {
        return CompletableFuture.supplyAsync(() -> {
            List<DictionaryEntryResponse> data = dictionaryService.listForHomebrew(DictionaryKind.fromSlug(kind), packageId);
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    /**
     * Создает результат операции "create dictionary" в рамках бизнес-логики API.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param kind входящее значение kind, используемое бизнес-сценарием
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/dictionaries/{kind}")
    @Operation(summary = "Create homebrew dictionary entry")
    public CompletableFuture<ResponseEntity<ApiResponse<DictionaryEntryResponse>>> createDictionary(
            @PathVariable UUID packageId,
            @PathVariable String kind,
            @Valid @RequestBody DictionaryEntryRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            DictionaryEntryResponse data = dictionaryService.createHomebrew(DictionaryKind.fromSlug(kind), packageId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(data, "Dictionary entry created"));
        }, controllerTaskExecutor);
    }

    /**
     * Обновляет результат операции "update dictionary" в рамках бизнес-логики API.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param kind входящее значение kind, используемое бизнес-сценарием
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PutMapping("/dictionaries/{kind}/{id}")
    @Operation(summary = "Update homebrew dictionary entry")
    public CompletableFuture<ResponseEntity<ApiResponse<DictionaryEntryResponse>>> updateDictionary(
            @PathVariable UUID packageId,
            @PathVariable String kind,
            @PathVariable UUID id,
            @Valid @RequestBody DictionaryEntryRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            DictionaryEntryResponse data = dictionaryService.updateHomebrew(DictionaryKind.fromSlug(kind), packageId, id, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Dictionary entry updated"));
        }, controllerTaskExecutor);
    }

    /**
     * Удаляет результат операции "delete dictionary" в рамках бизнес-логики API.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param kind входящее значение kind, используемое бизнес-сценарием
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/dictionaries/{kind}/{id}")
    @Operation(summary = "Delete homebrew dictionary entry")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteDictionary(
            @PathVariable UUID packageId,
            @PathVariable String kind,
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            dictionaryService.deleteHomebrew(DictionaryKind.fromSlug(kind), packageId, id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Dictionary entry deleted"));
        }, controllerTaskExecutor);
    }

    // === Homebrew monsters ===

    /**
     * Возвращает список для операции "list monsters" в рамках бизнес-логики API.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/monsters")
    @Operation(summary = "List homebrew monsters")
    public CompletableFuture<ResponseEntity<ApiResponse<List<MonsterSummaryResponse>>>> listMonsters(
            @PathVariable UUID packageId, Authentication auth,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() -> {
            List<MonsterSummaryResponse> data = monsterService.listHomebrewMonsters(packageId, auth.getName(), lang);
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get monster" в рамках бизнес-логики API.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/monsters/{id}")
    @Operation(summary = "Get monster details")
    public CompletableFuture<ResponseEntity<ApiResponse<MonsterResponse>>> getMonster(
            @PathVariable UUID packageId,
            @PathVariable UUID id, Authentication auth,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() -> {
            MonsterResponse data = monsterService.getMonster(id, auth.getName(), lang);
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    /**
     * Создает результат операции "create monster" в рамках бизнес-логики API.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/monsters")
    @Operation(summary = "Create homebrew monster")
    public CompletableFuture<ResponseEntity<ApiResponse<MonsterResponse>>> createMonster(
            @PathVariable UUID packageId,
            @Valid @RequestBody MonsterRequest request, Authentication auth,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() -> {
            MonsterResponse data = monsterService.createHomebrewMonster(packageId, request, auth.getName(), lang);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(data, "Monster created"));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "duplicate monster" в рамках бизнес-логики API.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param sourceId идентификатор source, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/monsters/duplicate/{sourceId}")
    @Operation(summary = "Duplicate a system or homebrew monster into this package")
    public CompletableFuture<ResponseEntity<ApiResponse<MonsterResponse>>> duplicateMonster(
            @PathVariable UUID packageId,
            @PathVariable UUID sourceId, Authentication auth,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() -> {
            MonsterResponse data = monsterService.duplicateMonsterIntoHomebrew(packageId, sourceId, auth.getName(), lang);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(data, "Monster duplicated"));
        }, controllerTaskExecutor);
    }

    /**
     * Обновляет результат операции "update monster" в рамках бизнес-логики API.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PutMapping("/monsters/{id}")
    @Operation(summary = "Update homebrew monster")
    public CompletableFuture<ResponseEntity<ApiResponse<MonsterResponse>>> updateMonster(
            @PathVariable UUID packageId,
            @PathVariable UUID id,
            @Valid @RequestBody MonsterRequest request, Authentication auth,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() -> {
            MonsterResponse data = monsterService.updateHomebrewMonster(packageId, id, request, auth.getName(), lang);
            return ResponseEntity.ok(ApiResponse.ok(data, "Monster updated"));
        }, controllerTaskExecutor);
    }

    /**
     * Удаляет результат операции "delete monster" в рамках бизнес-логики API.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/monsters/{id}")
    @Operation(summary = "Delete homebrew monster")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteMonster(
            @PathVariable UUID packageId,
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            monsterService.deleteHomebrewMonster(packageId, id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Monster deleted"));
        }, controllerTaskExecutor);
    }
}
