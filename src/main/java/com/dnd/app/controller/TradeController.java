package com.dnd.app.controller;

import com.dnd.app.dto.request.AddShopItemRequest;
import com.dnd.app.dto.request.BuyItemRequest;
import com.dnd.app.dto.request.SellItemRequest;
import com.dnd.app.dto.request.UpdateShopSettingsRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.ShopItemResponse;
import com.dnd.app.dto.response.ShopSettingsResponse;
import com.dnd.app.dto.response.TradeResultResponse;
import com.dnd.app.service.TradeService;
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
 * Класс TradeController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequestMapping("/api/campaigns/{campaignId}/npcs/{npcId}/shop")
@RequiredArgsConstructor
@Tag(name = "Trading", description = "Merchant NPC shop: browse, stock, buy and sell")
public class TradeController {

    private final TradeService tradeService;
    private final Executor controllerTaskExecutor;

    /**
     * Возвращает список для операции "list shop" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping
    @Operation(summary = "List a merchant's shop inventory (members)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ShopItemResponse>>>> listShop(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<ShopItemResponse> data = tradeService.listShop(campaignId, npcId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "stock shop" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/stock")
    @Operation(summary = "Stock the merchant's shop with an item (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<ShopItemResponse>>> stockShop(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId,
            @Valid @RequestBody AddShopItemRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            ShopItemResponse data = tradeService.stockShop(campaignId, npcId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(data, "Shop updated"));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "buy" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    /**
     * Удаляет позицию из витрины торговца (только ГМ).
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param shopItemId идентификатор позиции витрины
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/stock/{shopItemId}")
    @Operation(summary = "Remove an item from the merchant's shop (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> removeShopItem(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId,
            @PathVariable UUID shopItemId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            tradeService.removeShopItem(campaignId, npcId, shopItemId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Shop item removed"));
        }, controllerTaskExecutor);
    }

    /**
     * Восстанавливает запас витрины по базовым значениям (только ГМ).
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/restock")
    @Operation(summary = "Restock the merchant's shop to its baseline quantities (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ShopItemResponse>>>> restockShop(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<ShopItemResponse> data = tradeService.restockShop(campaignId, npcId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Shop restocked"));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает опциональные настройки экономики торговца (участники кампании).
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/settings")
    @Operation(summary = "Get the merchant's optional economy settings (members)")
    public CompletableFuture<ResponseEntity<ApiResponse<ShopSettingsResponse>>> getShopSettings(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            ShopSettingsResponse data = tradeService.getShopSettings(campaignId, npcId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    /**
     * Обновляет опциональные настройки экономики торговца (только ГМ).
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PutMapping("/settings")
    @Operation(summary = "Update the merchant's optional economy settings (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<ShopSettingsResponse>>> updateShopSettings(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId,
            @Valid @RequestBody UpdateShopSettingsRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            ShopSettingsResponse data = tradeService.updateShopSettings(campaignId, npcId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Shop settings updated"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/buy")
    @Operation(summary = "Buy an item from the merchant for one of your characters")
    public CompletableFuture<ResponseEntity<ApiResponse<TradeResultResponse>>> buy(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId,
            @Valid @RequestBody BuyItemRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            TradeResultResponse data = tradeService.buy(campaignId, npcId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Purchase complete"));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "sell" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/sell")
    @Operation(summary = "Sell a carried item to the merchant")
    public CompletableFuture<ResponseEntity<ApiResponse<TradeResultResponse>>> sell(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId,
            @Valid @RequestBody SellItemRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            TradeResultResponse data = tradeService.sell(campaignId, npcId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Sale complete"));
        }, controllerTaskExecutor);
    }
}
