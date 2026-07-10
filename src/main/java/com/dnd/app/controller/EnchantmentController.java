package com.dnd.app.controller;

import com.dnd.app.dto.request.AddEnchantmentRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.EnchantmentResponse;
import com.dnd.app.service.EnchantmentService;
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
 * Класс EnchantmentController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequestMapping("/api/campaigns/{campaignId}/characters/{characterId}/inventory/{instanceId}/enchantments")
@RequiredArgsConstructor
@Tag(name = "Item Enchantments", description = "Enchantment management for item instances")
public class EnchantmentController {

    private final EnchantmentService enchantmentService;
    private final Executor controllerTaskExecutor;

    /**
     * Возвращает результат операции "get item enchantments" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param instanceId идентификатор instance, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping
    @Operation(summary = "Get enchantments on item instance")
    public CompletableFuture<ResponseEntity<ApiResponse<List<EnchantmentResponse>>>> getItemEnchantments(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId,
            @PathVariable UUID instanceId, Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(
                        enchantmentService.getItemEnchantments(characterId, instanceId, auth.getName()))),
                controllerTaskExecutor);
    }

    /**
     * Добавляет результат операции "add enchantment" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param instanceId идентификатор instance, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping
    @Operation(summary = "Add enchantment to item instance")
    public CompletableFuture<ResponseEntity<ApiResponse<EnchantmentResponse>>> addEnchantment(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId,
            @PathVariable UUID instanceId,
            @Valid @RequestBody AddEnchantmentRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok(
                                enchantmentService.addItemEnchantment(characterId, instanceId, request, auth.getName()),
                                "Зачарование наложено")),
                controllerTaskExecutor);
    }

    /**
     * Удаляет результат операции "remove enchantment" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param instanceId идентификатор instance, используемый для выбора нужного бизнес-объекта
     * @param enchantmentId идентификатор enchantment, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/{enchantmentId}")
    @Operation(summary = "Remove enchantment from item instance")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> removeEnchantment(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId,
            @PathVariable UUID instanceId,
            @PathVariable UUID enchantmentId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            enchantmentService.removeItemEnchantment(characterId, enchantmentId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Зачарование снято"));
        }, controllerTaskExecutor);
    }
}
