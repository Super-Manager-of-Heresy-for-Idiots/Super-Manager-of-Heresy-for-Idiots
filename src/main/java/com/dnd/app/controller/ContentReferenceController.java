package com.dnd.app.controller;

import com.dnd.app.dto.content.ContentClassDetailResponse;
import com.dnd.app.dto.content.SpeciesDetailResponse;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.service.ContentReferenceService;
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
 * Класс ContentReferenceController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Content Reference", description = "New normalized content model (classes, features, reward groups)")
public class ContentReferenceController {

    private final ContentReferenceService contentReferenceService;
    private final Executor controllerTaskExecutor;

    /**
     * Возвращает результат операции "get vanilla classes" в рамках бизнес-логики API.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({"/api/reference/classes", "/api/reference/content/classes"})
    @Operation(summary = "Get core (vanilla) classes from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ContentClassDetailResponse>>>> getVanillaClasses(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentReferenceService.getVanillaClasses(lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get vanilla class" в рамках бизнес-логики API.
     * @param classId идентификатор class, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({"/api/reference/classes/{classId}", "/api/reference/content/classes/{classId}"})
    @Operation(summary = "Get a single core (vanilla) class from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<ContentClassDetailResponse>>> getVanillaClass(
            @PathVariable UUID classId,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentReferenceService.getVanillaClass(classId, lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get campaign classes" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({
            "/api/campaigns/{campaignId}/reference/classes",
            "/api/campaigns/{campaignId}/reference/content/classes"
    })
    @Operation(summary = "Get campaign-visible classes (core + active homebrew) from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ContentClassDetailResponse>>>> getCampaignClasses(
            @PathVariable UUID campaignId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                contentReferenceService.getCampaignClasses(campaignId, auth.getName(), lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get campaign class" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param classId идентификатор class, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({
            "/api/campaigns/{campaignId}/reference/classes/{classId}",
            "/api/campaigns/{campaignId}/reference/content/classes/{classId}"
    })
    @Operation(summary = "Get a single campaign-visible class from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<ContentClassDetailResponse>>> getCampaignClass(
            @PathVariable UUID campaignId,
            @PathVariable UUID classId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                contentReferenceService.getCampaignClass(campaignId, classId, auth.getName(), lang))),
                controllerTaskExecutor);
    }

    // --- species (D&D 2024 race replacement) ---

    /**
     * Возвращает результат операции "get vanilla species" в рамках бизнес-логики API.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({"/api/reference/species", "/api/reference/content/species"})
    @Operation(summary = "Get core (vanilla) species from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<List<SpeciesDetailResponse>>>> getVanillaSpecies(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentReferenceService.getVanillaSpecies(lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get vanilla species by id" в рамках бизнес-логики API.
     * @param speciesId идентификатор species, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({"/api/reference/species/{speciesId}", "/api/reference/content/species/{speciesId}"})
    @Operation(summary = "Get a single core (vanilla) species from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<SpeciesDetailResponse>>> getVanillaSpeciesById(
            @PathVariable UUID speciesId,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentReferenceService.getVanillaSpeciesById(speciesId, lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get campaign species" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({
            "/api/campaigns/{campaignId}/reference/species",
            "/api/campaigns/{campaignId}/reference/content/species"
    })
    @Operation(summary = "Get campaign-visible species (core + active homebrew) from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<List<SpeciesDetailResponse>>>> getCampaignSpecies(
            @PathVariable UUID campaignId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                contentReferenceService.getCampaignSpecies(campaignId, auth.getName(), lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get campaign species by id" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param speciesId идентификатор species, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping({
            "/api/campaigns/{campaignId}/reference/species/{speciesId}",
            "/api/campaigns/{campaignId}/reference/content/species/{speciesId}"
    })
    @Operation(summary = "Get a single campaign-visible species from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<SpeciesDetailResponse>>> getCampaignSpeciesById(
            @PathVariable UUID campaignId,
            @PathVariable UUID speciesId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                contentReferenceService.getCampaignSpeciesById(campaignId, speciesId, auth.getName(), lang))),
                controllerTaskExecutor);
    }
}
