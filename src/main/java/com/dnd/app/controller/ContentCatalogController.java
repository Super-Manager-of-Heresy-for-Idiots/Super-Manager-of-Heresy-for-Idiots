package com.dnd.app.controller;

import com.dnd.app.dto.content.BackgroundDetailResponse;
import com.dnd.app.dto.content.EquipmentItemDetailResponse;
import com.dnd.app.dto.content.FeatDetailResponse;
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
 * Read-only catalog endpoints for the normalized content model (feats, spells,
 * backgrounds, equipment, magic items). Mirrors {@link ContentReferenceController}
 * for classes and species.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Content Catalog", description = "New normalized content model (feats, spells, backgrounds, items)")
public class ContentCatalogController {

    private final ContentCatalogService contentCatalogService;
    private final Executor controllerTaskExecutor;

    // --- feats ---

    @GetMapping({"/api/reference/feats", "/api/reference/content/feats"})
    @Operation(summary = "Get core (vanilla) feats from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<List<FeatDetailResponse>>>> getVanillaFeats(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentCatalogService.getVanillaFeats(lang))),
                controllerTaskExecutor);
    }

    @GetMapping({"/api/reference/feats/{featId}", "/api/reference/content/feats/{featId}"})
    @Operation(summary = "Get a single core (vanilla) feat from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatDetailResponse>>> getVanillaFeat(
            @PathVariable UUID featId,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentCatalogService.getVanillaFeat(featId, lang))),
                controllerTaskExecutor);
    }

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

    @GetMapping({"/api/reference/spells", "/api/reference/content/spells"})
    @Operation(summary = "Get core (vanilla) spells from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<List<SpellDetailResponse>>>> getVanillaSpells(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentCatalogService.getVanillaSpells(lang))),
                controllerTaskExecutor);
    }

    @GetMapping({"/api/reference/spells/{spellId}", "/api/reference/content/spells/{spellId}"})
    @Operation(summary = "Get a single core (vanilla) spell from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<SpellDetailResponse>>> getVanillaSpell(
            @PathVariable UUID spellId,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentCatalogService.getVanillaSpell(spellId, lang))),
                controllerTaskExecutor);
    }

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

    @GetMapping({"/api/reference/backgrounds", "/api/reference/content/backgrounds"})
    @Operation(summary = "Get core (vanilla) backgrounds from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<List<BackgroundDetailResponse>>>> getVanillaBackgrounds(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentCatalogService.getVanillaBackgrounds(lang))),
                controllerTaskExecutor);
    }

    @GetMapping({"/api/reference/backgrounds/{backgroundId}", "/api/reference/content/backgrounds/{backgroundId}"})
    @Operation(summary = "Get a single core (vanilla) background from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<BackgroundDetailResponse>>> getVanillaBackground(
            @PathVariable UUID backgroundId,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentCatalogService.getVanillaBackground(backgroundId, lang))),
                controllerTaskExecutor);
    }

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

    @GetMapping({"/api/reference/equipment", "/api/reference/content/equipment"})
    @Operation(summary = "Get core (vanilla) equipment items from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<List<EquipmentItemDetailResponse>>>> getVanillaEquipmentItems(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentCatalogService.getVanillaEquipmentItems(lang))),
                controllerTaskExecutor);
    }

    @GetMapping({"/api/reference/equipment/{equipmentItemId}", "/api/reference/content/equipment/{equipmentItemId}"})
    @Operation(summary = "Get a single core (vanilla) equipment item from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<EquipmentItemDetailResponse>>> getVanillaEquipmentItem(
            @PathVariable UUID equipmentItemId,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentCatalogService.getVanillaEquipmentItem(equipmentItemId, lang))),
                controllerTaskExecutor);
    }

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

    @GetMapping({"/api/reference/magic-items", "/api/reference/content/magic-items"})
    @Operation(summary = "Get core (vanilla) magic items from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<List<MagicItemDetailResponse>>>> getVanillaMagicItems(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentCatalogService.getVanillaMagicItems(lang))),
                controllerTaskExecutor);
    }

    @GetMapping({"/api/reference/magic-items/{magicItemId}", "/api/reference/content/magic-items/{magicItemId}"})
    @Operation(summary = "Get a single core (vanilla) magic item from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<MagicItemDetailResponse>>> getVanillaMagicItem(
            @PathVariable UUID magicItemId,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentCatalogService.getVanillaMagicItem(magicItemId, lang))),
                controllerTaskExecutor);
    }

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
}
