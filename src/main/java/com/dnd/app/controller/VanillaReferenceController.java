package com.dnd.app.controller;

import com.dnd.app.dto.response.*;
import com.dnd.app.dto.content.ContentLabelDto;
import com.dnd.app.dto.content.FeatOptionDto;
import com.dnd.app.dto.content.ModifierKeyDto;
import com.dnd.app.service.ReferenceDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Класс VanillaReferenceController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequestMapping("/api/reference")
@RequiredArgsConstructor
@Tag(name = "Vanilla Reference Data", description = "System 5e reference data for character templates (no campaign)")
public class VanillaReferenceController {

    private final ReferenceDataService referenceDataService;
    private final Executor controllerTaskExecutor;

    // Legacy vanilla class reference endpoint removed in Phase 12 — superseded by
    // GET /api/reference/content/classes (ContentReferenceController).

    // Legacy vanilla race reference endpoint removed in S5 — superseded by
    // GET /api/reference/content/species (ContentReferenceController).

    /**
     * Возвращает результат операции "get backgrounds" в рамках бизнес-логики API.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/backgrounds")
    @Operation(summary = "Get vanilla (system) backgrounds for character templates")
    public CompletableFuture<ResponseEntity<ApiResponse<List<BackgroundResponse>>>> getBackgrounds(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(referenceDataService.getVanillaBackgrounds(lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get skills" в рамках бизнес-логики API.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/skills")
    @Operation(summary = "Get 18 proficiency skills")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ProficiencySkillResponse>>>> getSkills(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(referenceDataService.getVanillaSkills(lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get stat types" в рамках бизнес-логики API.
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/stat-types")
    @Operation(summary = "Get 6 ability score types")
    public CompletableFuture<ResponseEntity<ApiResponse<List<StatTypeResponse>>>> getStatTypes() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(referenceDataService.getVanillaStatTypes())),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get currencies" в рамках бизнес-логики API.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/currencies")
    @Operation(summary = "Get vanilla (system) currency types")
    public CompletableFuture<ResponseEntity<ApiResponse<List<CurrencyTypeResponse>>>> getCurrencies(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(referenceDataService.getVanillaCurrencies(lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get spells" в рамках бизнес-логики API.
     * @param classId идентификатор class, используемый для выбора нужного бизнес-объекта
     * @param level входящее значение level, используемое бизнес-сценарием
     * @param school входящее значение school, используемое бизнес-сценарием
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/spells")
    @Operation(summary = "Get vanilla (system) spells with optional filters")
    public CompletableFuture<ResponseEntity<ApiResponse<List<SpellResponse>>>> getSpells(
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) String school,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(
                        referenceDataService.getVanillaSpells(classId, level, school, lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get abilities" в рамках бизнес-логики API.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/abilities")
    @Operation(summary = "Get ability scores (ability_score) for authoring dropdowns")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ContentLabelDto>>>> getAbilities(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(referenceDataService.getVanillaAbilities(lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get feats" в рамках бизнес-логики API.
     * @param query входящее значение query, используемое бизнес-сценарием
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/feats")
    @Operation(summary = "Get feats (searchable) for authoring dropdowns")
    public CompletableFuture<ResponseEntity<ApiResponse<List<FeatOptionDto>>>> getFeats(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(referenceDataService.getVanillaFeats(query, lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get modifier keys" в рамках бизнес-логики API.
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/modifier-keys")
    @Operation(summary = "Get known numeric-modifier keys (free text still allowed)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ModifierKeyDto>>>> getModifierKeys() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(referenceDataService.getModifierKeys())),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get rarities" в рамках бизнес-логики API.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/rarities")
    @Operation(summary = "Get magic item rarities for item-authoring dropdowns")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ContentLabelDto>>>> getRarities(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(referenceDataService.getRarities(lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get damage types" в рамках бизнес-логики API.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/damage-types")
    @Operation(summary = "Get damage types (PHB) for item/spell/skill-authoring dropdowns")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ContentLabelDto>>>> getDamageTypes(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(referenceDataService.getDamageTypes(lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get conditions" в рамках бизнес-логики API.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/conditions")
    @Operation(summary = "Get combat conditions (Blinded, Prone, …) for the battle tracker")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ContentLabelDto>>>> getConditions(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(referenceDataService.getConditions(lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает словарь событий-триггеров движка для пикера триггера реакции homebrew-заклинания (HB_UX Фаза 1).
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/reaction-triggers")
    @Operation(summary = "Get engine gameplay-event triggers for the spell reaction-trigger picker")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ContentLabelDto>>>> getReactionTriggers(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(referenceDataService.getReactionTriggers(lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get spell schools" в рамках бизнес-логики API.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/spell-schools")
    @Operation(summary = "Get spell schools for spell-authoring/filter dropdowns")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ContentLabelDto>>>> getSpellSchools(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(referenceDataService.getSpellSchools(lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get sizes" в рамках бизнес-логики API.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/sizes")
    @Operation(summary = "Get creature sizes (character_size) for race/character dropdowns")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ContentLabelDto>>>> getSizes(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(referenceDataService.getSizes(lang))),
                controllerTaskExecutor);
    }
}
