package com.dnd.app.controller;

import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.SpellSlotsResponse;
import com.dnd.app.service.SpellSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Класс SpellSlotController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Spell Slots", description = "Per-character spell slot consumption and recovery")
public class SpellSlotController {

    private final SpellSlotService spellSlotService;
    private final Executor controllerTaskExecutor;

    /**
     * Возвращает результат операции "get slots" в рамках бизнес-логики API.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/api/characters/{characterId}/spell-slots")
    @Operation(summary = "Get derived max, expended and available spell slots per spell level")
    public CompletableFuture<ResponseEntity<ApiResponse<SpellSlotsResponse>>> getSlots(
            @PathVariable UUID characterId,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                spellSlotService.getSlots(characterId, auth.getName()))),
                controllerTaskExecutor);
    }

    /**
     * Выполняет операции "expend" в рамках бизнес-логики API.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param spellLevel входящее значение spell level, используемое бизнес-сценарием
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/api/characters/{characterId}/spell-slots/{spellLevel}/expend")
    @Operation(summary = "Expend one spell slot of the given level")
    public CompletableFuture<ResponseEntity<ApiResponse<SpellSlotsResponse>>> expend(
            @PathVariable UUID characterId,
            @PathVariable int spellLevel,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                spellSlotService.expend(characterId, auth.getName(), spellLevel),
                                "Ячейка заклинания потрачена")),
                controllerTaskExecutor);
    }

    /**
     * Выполняет операции "restore one" в рамках бизнес-логики API.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param spellLevel входящее значение spell level, используемое бизнес-сценарием
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/api/characters/{characterId}/spell-slots/{spellLevel}/restore")
    @Operation(summary = "Restore one expended spell slot of the given level (GM granular adjust)")
    public CompletableFuture<ResponseEntity<ApiResponse<SpellSlotsResponse>>> restoreOne(
            @PathVariable UUID characterId,
            @PathVariable int spellLevel,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                spellSlotService.restoreOne(characterId, auth.getName(), spellLevel),
                                "Ячейка заклинания восстановлена")),
                controllerTaskExecutor);
    }

    /**
     * Выполняет операции "restore all" в рамках бизнес-логики API.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/api/characters/{characterId}/spell-slots/restore-all")
    @Operation(summary = "Restore all expended spell slots (long rest)")
    public CompletableFuture<ResponseEntity<ApiResponse<SpellSlotsResponse>>> restoreAll(
            @PathVariable UUID characterId,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                spellSlotService.restoreAll(characterId, auth.getName()),
                                "Все ячейки заклинаний восстановлены")),
                controllerTaskExecutor);
    }

    /**
     * Выполняет операции "restore half" в рамках бизнес-логики API.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/api/characters/{characterId}/spell-slots/restore-half")
    @Operation(summary = "Restore half of the expended spell slots (partial recovery)")
    public CompletableFuture<ResponseEntity<ApiResponse<SpellSlotsResponse>>> restoreHalf(
            @PathVariable UUID characterId,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                spellSlotService.restoreHalf(characterId, auth.getName()),
                                "Половина использованных ячеек восстановлена")),
                controllerTaskExecutor);
    }
}
