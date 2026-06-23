package com.dnd.app.controller;

import com.dnd.app.dto.request.AddShopItemRequest;
import com.dnd.app.dto.request.BuyItemRequest;
import com.dnd.app.dto.request.SellItemRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.ShopItemResponse;
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
 * Trading with a merchant NPC's shop. The GM stocks the shop; campaign members buy from and sell
 * to it with their characters. Prices are in gold pieces and settle through the character wallet.
 */
@RestController
@RequestMapping("/api/campaigns/{campaignId}/npcs/{npcId}/shop")
@RequiredArgsConstructor
@Tag(name = "Trading", description = "Merchant NPC shop: browse, stock, buy and sell")
public class TradeController {

    private final TradeService tradeService;
    private final Executor controllerTaskExecutor;

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
