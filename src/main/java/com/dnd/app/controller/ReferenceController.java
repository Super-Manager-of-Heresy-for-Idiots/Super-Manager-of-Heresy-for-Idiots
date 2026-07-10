package com.dnd.app.controller;

import com.dnd.app.dto.response.*;
import com.dnd.app.service.ReferenceDataService;
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
 * Класс ReferenceController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequestMapping("/api/campaigns/{campaignId}/reference")
@RequiredArgsConstructor
@Tag(name = "Reference Data", description = "5e reference data for character wizard")
public class ReferenceController {

    private final ReferenceDataService referenceDataService;
    private final Executor controllerTaskExecutor;

    // Legacy class reference endpoint removed in Phase 12 — superseded by
    // GET /api/campaigns/{campaignId}/reference/content/classes (ContentReferenceController).

    // Legacy race reference endpoint removed in S5 — superseded by
    // GET /api/campaigns/{campaignId}/reference/content/species (ContentReferenceController).

    /**
     * Возвращает результат операции "get backgrounds" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/backgrounds")
    @Operation(summary = "Get available backgrounds")
    public CompletableFuture<ResponseEntity<ApiResponse<List<BackgroundResponse>>>> getBackgrounds(
            @PathVariable UUID campaignId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(
                        referenceDataService.getBackgrounds(campaignId, auth.getName(), lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get skills" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/skills")
    @Operation(summary = "Get 18 proficiency skills")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ProficiencySkillResponse>>>> getSkills(
            @PathVariable UUID campaignId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(
                        referenceDataService.getSkills(campaignId, auth.getName(), lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get stat types" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/stat-types")
    @Operation(summary = "Get 6 ability score types")
    public CompletableFuture<ResponseEntity<ApiResponse<List<StatTypeResponse>>>> getStatTypes(
            @PathVariable UUID campaignId, Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(
                        referenceDataService.getStatTypes(campaignId, auth.getName()))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get currencies" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/currencies")
    @Operation(summary = "Get available currency types")
    public CompletableFuture<ResponseEntity<ApiResponse<List<CurrencyTypeResponse>>>> getCurrencies(
            @PathVariable UUID campaignId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(
                        referenceDataService.getCurrencies(campaignId, auth.getName(), lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get spells" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param classId идентификатор class, используемый для выбора нужного бизнес-объекта
     * @param level входящее значение level, используемое бизнес-сценарием
     * @param school входящее значение school, используемое бизнес-сценарием
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/spells")
    @Operation(summary = "Get spells with optional filters")
    public CompletableFuture<ResponseEntity<ApiResponse<List<SpellResponse>>>> getSpells(
            @PathVariable UUID campaignId,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) String school,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(
                        referenceDataService.getSpells(campaignId, auth.getName(), classId, level, school, lang))),
                controllerTaskExecutor);
    }
}
