package com.dnd.app.controller;

import com.dnd.app.dto.content.BackgroundDetailResponse;
import com.dnd.app.dto.content.EquipmentItemDetailResponse;
import com.dnd.app.dto.content.FeatDetailResponse;
import com.dnd.app.dto.content.ItemDefinitionResponse;
import com.dnd.app.dto.content.MagicItemDetailResponse;
import com.dnd.app.dto.content.SpellDetailResponse;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.service.ContentCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Класс ContentCatalogController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Content Catalog", description = "New normalized content model (feats, spells, backgrounds, items)")
public class ContentCatalogController {

    private final ContentCatalogService contentCatalogService;
    private final Executor controllerTaskExecutor;

    // --- feats ---

    /**
     * Возвращает результат операции "get vanilla feats" в рамках бизнес-логики API.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({"/api/reference/feats", "/api/reference/content/feats"})
    @Operation(summary = "Get core (vanilla) feats from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<List<FeatDetailResponse>>>> getVanillaFeats(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentCatalogService.getVanillaFeats(lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get vanilla feat" в рамках бизнес-логики API.
     * @param featId идентификатор feat, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({"/api/reference/feats/{featId}", "/api/reference/content/feats/{featId}"})
    @Operation(summary = "Get a single core (vanilla) feat from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatDetailResponse>>> getVanillaFeat(
            @PathVariable UUID featId,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentCatalogService.getVanillaFeat(featId, lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get campaign feats" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({
            "/api/campaigns/{campaignId}/reference/feats",
            "/api/campaigns/{campaignId}/reference/content/feats"
    })
    @Operation(summary = "Get campaign-visible feats (core + active homebrew) from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<List<FeatDetailResponse>>>> getCampaignFeats(
            @PathVariable UUID campaignId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                contentCatalogService.getCampaignFeats(campaignId, auth.getName(), lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get campaign feat" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param featId идентификатор feat, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({
            "/api/campaigns/{campaignId}/reference/feats/{featId}",
            "/api/campaigns/{campaignId}/reference/content/feats/{featId}"
    })
    @Operation(summary = "Get a single campaign-visible feat from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatDetailResponse>>> getCampaignFeat(
            @PathVariable UUID campaignId,
            @PathVariable UUID featId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                contentCatalogService.getCampaignFeat(campaignId, featId, auth.getName(), lang))),
                controllerTaskExecutor);
    }

    // --- spells ---

    /**
     * Возвращает результат операции "get vanilla spells" в рамках бизнес-логики API.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({"/api/reference/spells", "/api/reference/content/spells"})
    @Operation(summary = "Get core (vanilla) spells from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<List<SpellDetailResponse>>>> getVanillaSpells(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentCatalogService.getVanillaSpells(lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get vanilla spell" в рамках бизнес-логики API.
     * @param spellId идентификатор spell, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({"/api/reference/spells/{spellId}", "/api/reference/content/spells/{spellId}"})
    @Operation(summary = "Get a single core (vanilla) spell from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<SpellDetailResponse>>> getVanillaSpell(
            @PathVariable UUID spellId,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentCatalogService.getVanillaSpell(spellId, lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get campaign spells" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({
            "/api/campaigns/{campaignId}/reference/spells",
            "/api/campaigns/{campaignId}/reference/content/spells"
    })
    @Operation(summary = "Get campaign-visible spells (core + active homebrew) from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<List<SpellDetailResponse>>>> getCampaignSpells(
            @PathVariable UUID campaignId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                contentCatalogService.getCampaignSpells(campaignId, auth.getName(), lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get campaign spell" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param spellId идентификатор spell, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({
            "/api/campaigns/{campaignId}/reference/spells/{spellId}",
            "/api/campaigns/{campaignId}/reference/content/spells/{spellId}"
    })
    @Operation(summary = "Get a single campaign-visible spell from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<SpellDetailResponse>>> getCampaignSpell(
            @PathVariable UUID campaignId,
            @PathVariable UUID spellId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                contentCatalogService.getCampaignSpell(campaignId, spellId, auth.getName(), lang))),
                controllerTaskExecutor);
    }

    // --- backgrounds ---

    /**
     * Возвращает результат операции "get vanilla backgrounds" в рамках бизнес-логики API.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({"/api/reference/backgrounds", "/api/reference/content/backgrounds"})
    @Operation(summary = "Get core (vanilla) backgrounds from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<List<BackgroundDetailResponse>>>> getVanillaBackgrounds(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentCatalogService.getVanillaBackgrounds(lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get vanilla background" в рамках бизнес-логики API.
     * @param backgroundId идентификатор background, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({"/api/reference/backgrounds/{backgroundId}", "/api/reference/content/backgrounds/{backgroundId}"})
    @Operation(summary = "Get a single core (vanilla) background from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<BackgroundDetailResponse>>> getVanillaBackground(
            @PathVariable UUID backgroundId,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentCatalogService.getVanillaBackground(backgroundId, lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get campaign backgrounds" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({
            "/api/campaigns/{campaignId}/reference/backgrounds",
            "/api/campaigns/{campaignId}/reference/content/backgrounds"
    })
    @Operation(summary = "Get campaign-visible backgrounds (core + active homebrew) from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<List<BackgroundDetailResponse>>>> getCampaignBackgrounds(
            @PathVariable UUID campaignId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                contentCatalogService.getCampaignBackgrounds(campaignId, auth.getName(), lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get campaign background" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param backgroundId идентификатор background, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({
            "/api/campaigns/{campaignId}/reference/backgrounds/{backgroundId}",
            "/api/campaigns/{campaignId}/reference/content/backgrounds/{backgroundId}"
    })
    @Operation(summary = "Get a single campaign-visible background from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<BackgroundDetailResponse>>> getCampaignBackground(
            @PathVariable UUID campaignId,
            @PathVariable UUID backgroundId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                contentCatalogService.getCampaignBackground(campaignId, backgroundId, auth.getName(), lang))),
                controllerTaskExecutor);
    }

    // --- equipment items ---

    /**
     * Возвращает результат операции "get vanilla equipment items" в рамках бизнес-логики API.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({"/api/reference/equipment", "/api/reference/content/equipment"})
    @Operation(summary = "Get core (vanilla) equipment items from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<List<EquipmentItemDetailResponse>>>> getVanillaEquipmentItems(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentCatalogService.getVanillaEquipmentItems(lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get vanilla equipment item" в рамках бизнес-логики API.
     * @param equipmentItemId идентификатор equipment item, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({"/api/reference/equipment/{equipmentItemId}", "/api/reference/content/equipment/{equipmentItemId}"})
    @Operation(summary = "Get a single core (vanilla) equipment item from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<EquipmentItemDetailResponse>>> getVanillaEquipmentItem(
            @PathVariable UUID equipmentItemId,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentCatalogService.getVanillaEquipmentItem(equipmentItemId, lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get campaign equipment items" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({
            "/api/campaigns/{campaignId}/reference/equipment",
            "/api/campaigns/{campaignId}/reference/content/equipment"
    })
    @Operation(summary = "Get campaign-visible equipment items (core + active homebrew) from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<List<EquipmentItemDetailResponse>>>> getCampaignEquipmentItems(
            @PathVariable UUID campaignId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                contentCatalogService.getCampaignEquipmentItems(campaignId, auth.getName(), lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get campaign equipment item" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param equipmentItemId идентификатор equipment item, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({
            "/api/campaigns/{campaignId}/reference/equipment/{equipmentItemId}",
            "/api/campaigns/{campaignId}/reference/content/equipment/{equipmentItemId}"
    })
    @Operation(summary = "Get a single campaign-visible equipment item from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<EquipmentItemDetailResponse>>> getCampaignEquipmentItem(
            @PathVariable UUID campaignId,
            @PathVariable UUID equipmentItemId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                contentCatalogService.getCampaignEquipmentItem(campaignId, equipmentItemId, auth.getName(), lang))),
                controllerTaskExecutor);
    }

    // --- magic items ---

    /**
     * Возвращает результат операции "get vanilla magic items" в рамках бизнес-логики API.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({"/api/reference/magic-items", "/api/reference/content/magic-items"})
    @Operation(summary = "Get core (vanilla) magic items from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<List<MagicItemDetailResponse>>>> getVanillaMagicItems(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentCatalogService.getVanillaMagicItems(lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get vanilla magic item" в рамках бизнес-логики API.
     * @param magicItemId идентификатор magic item, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({"/api/reference/magic-items/{magicItemId}", "/api/reference/content/magic-items/{magicItemId}"})
    @Operation(summary = "Get a single core (vanilla) magic item from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<MagicItemDetailResponse>>> getVanillaMagicItem(
            @PathVariable UUID magicItemId,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentCatalogService.getVanillaMagicItem(magicItemId, lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get campaign magic items" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({
            "/api/campaigns/{campaignId}/reference/magic-items",
            "/api/campaigns/{campaignId}/reference/content/magic-items"
    })
    @Operation(summary = "Get campaign-visible magic items (core + active homebrew) from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<List<MagicItemDetailResponse>>>> getCampaignMagicItems(
            @PathVariable UUID campaignId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                contentCatalogService.getCampaignMagicItems(campaignId, auth.getName(), lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get campaign magic item" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param magicItemId идентификатор magic item, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({
            "/api/campaigns/{campaignId}/reference/magic-items/{magicItemId}",
            "/api/campaigns/{campaignId}/reference/content/magic-items/{magicItemId}"
    })
    @Operation(summary = "Get a single campaign-visible magic item from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<MagicItemDetailResponse>>> getCampaignMagicItem(
            @PathVariable UUID campaignId,
            @PathVariable UUID magicItemId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                contentCatalogService.getCampaignMagicItem(campaignId, magicItemId, auth.getName(), lang))),
                controllerTaskExecutor);
    }

    // --- unified item definitions (IT-1): equipment + magic + legacy template ---

    /**
     * Возвращает единый ванильный каталог «Предметов» (IT-1): снаряжение + магические + легаси-шаблоны в одном списке.
     * @param lang язык локализации меток
     * @return унифицированный список определений предметов
     */
    @GetMapping({"/api/reference/items", "/api/reference/content/items"})
    @Operation(summary = "Get core (vanilla) unified item definitions (equipment + magic + legacy template)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ItemDefinitionResponse>>>> getVanillaItems(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentCatalogService.getVanillaItems(lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает единое ванильное определение предмета по id (резолв по трём таблицам).
     * @param itemId идентификатор предмета
     * @param lang язык локализации меток
     * @return унифицированное определение предмета
     */
    @GetMapping({"/api/reference/items/{itemId}", "/api/reference/content/items/{itemId}"})
    @Operation(summary = "Get a single core (vanilla) unified item definition")
    public CompletableFuture<ResponseEntity<ApiResponse<ItemDefinitionResponse>>> getVanillaItem(
            @PathVariable UUID itemId,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentCatalogService.getVanillaItem(itemId, lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает единый каталог «Предметов» для кампании (IT-1): ваниль + активные homebrew-пакеты кампании.
     * @param campaignId идентификатор кампании
     * @param lang язык локализации меток
     * @param auth аутентификация (проверка доступа к кампании)
     * @return унифицированный список определений предметов, видимых в кампании
     */
    @GetMapping({
            "/api/campaigns/{campaignId}/reference/items",
            "/api/campaigns/{campaignId}/reference/content/items"
    })
    @Operation(summary = "Get campaign-visible unified item definitions (core + active homebrew)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ItemDefinitionResponse>>>> getCampaignItems(
            @PathVariable UUID campaignId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                contentCatalogService.getCampaignItems(campaignId, auth.getName(), lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает единое определение предмета в контексте кампании (резолв по трём таблицам + проверка видимости).
     * @param campaignId идентификатор кампании
     * @param itemId идентификатор предмета
     * @param lang язык локализации меток
     * @param auth аутентификация (проверка доступа)
     * @return унифицированное определение предмета
     */
    @GetMapping({
            "/api/campaigns/{campaignId}/reference/items/{itemId}",
            "/api/campaigns/{campaignId}/reference/content/items/{itemId}"
    })
    @Operation(summary = "Get a single campaign-visible unified item definition")
    public CompletableFuture<ResponseEntity<ApiResponse<ItemDefinitionResponse>>> getCampaignItem(
            @PathVariable UUID campaignId,
            @PathVariable UUID itemId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                contentCatalogService.getCampaignItem(campaignId, itemId, auth.getName(), lang))),
                controllerTaskExecutor);
    }
}
