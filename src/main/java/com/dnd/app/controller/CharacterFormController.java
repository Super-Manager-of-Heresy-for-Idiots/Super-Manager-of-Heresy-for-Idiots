package com.dnd.app.controller;

import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.dto.featurerule.CompanionResponse;
import com.dnd.app.dto.featurerule.KnownFormResponse;
import com.dnd.app.dto.featurerule.TacticalSnapshot;
import com.dnd.app.dto.featurerule.TransformationResponse;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.security.CharacterAccessGuard;
import com.dnd.app.service.CharacterFormService;
import com.dnd.app.service.FeatureCompanionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Класс CharacterFormController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequestMapping("/api/characters/{characterId}/features")
@RequiredArgsConstructor
@Tag(name = "Character Forms & Companions", description = "Wild Shape forms, transformations, companions")
public class CharacterFormController {

    private final CharacterFormService characterFormService;
    private final FeatureCompanionService featureCompanionService;
    private final CharacterAccessGuard accessGuard;
    private final Executor controllerTaskExecutor;

    // ── Known forms ─────────────────────────────────────────────────────────

    /**
     * Выполняет операции "forms" в рамках бизнес-логики API.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/forms")
    @Operation(summary = "List a character's known monster forms")
    public CompletableFuture<ResponseEntity<ApiResponse<List<KnownFormResponse>>>> forms(
            @PathVariable UUID characterId, Authentication authentication) {
        final String username = username(authentication);
        return CompletableFuture.supplyAsync(() -> {
            accessGuard.require(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(characterFormService.listKnownForms(characterId)));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "learn form" в рамках бизнес-логики API.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param monsterId идентификатор monster, используемый для выбора нужного бизнес-объекта
     * @param sourceFeatureId идентификатор source feature, используемый для выбора нужного бизнес-объекта
     * @param level входящее значение level, используемое бизнес-сценарием
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/forms")
    @Operation(summary = "Learn a monster form (checked against the feature's eligibility filter)")
    public CompletableFuture<ResponseEntity<ApiResponse<KnownFormResponse>>> learnForm(
            @PathVariable UUID characterId, @RequestParam UUID monsterId,
            @RequestParam(required = false) UUID sourceFeatureId, @RequestParam(required = false) Integer level,
            Authentication authentication) {
        final String username = username(authentication);
        return CompletableFuture.supplyAsync(() -> {
            PlayerCharacter c = accessGuard.require(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(
                    characterFormService.learnForm(c, monsterId, sourceFeatureId, level), "Форма изучена"));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "approve form" в рамках бизнес-логики API.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param formId идентификатор form, используемый для выбора нужного бизнес-объекта
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/forms/{formId}/approve")
    @Operation(summary = "GM: approve a known form")
    public CompletableFuture<ResponseEntity<ApiResponse<KnownFormResponse>>> approveForm(
            @PathVariable UUID characterId, @PathVariable UUID formId, Authentication authentication) {
        final String username = username(authentication);
        return CompletableFuture.supplyAsync(() -> {
            accessGuard.require(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(characterFormService.approveForm(formId), "Форма одобрена"));
        }, controllerTaskExecutor);
    }

    // ── Transformation ──────────────────────────────────────────────────────

    /**
     * Выполняет операции "current transformation" в рамках бизнес-логики API.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/transformation")
    @Operation(summary = "Get the character's current transformation (if any)")
    public CompletableFuture<ResponseEntity<ApiResponse<TransformationResponse>>> currentTransformation(
            @PathVariable UUID characterId, Authentication authentication) {
        final String username = username(authentication);
        return CompletableFuture.supplyAsync(() -> {
            accessGuard.require(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(characterFormService.currentTransformation(characterId)));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "transform" в рамках бизнес-логики API.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param monsterId идентификатор monster, используемый для выбора нужного бизнес-объекта
     * @param sourceFeatureId идентификатор source feature, используемый для выбора нужного бизнес-объекта
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/transformation")
    @Operation(summary = "Transform into a monster form")
    public CompletableFuture<ResponseEntity<ApiResponse<TransformationResponse>>> transform(
            @PathVariable UUID characterId, @RequestParam UUID monsterId,
            @RequestParam(required = false) UUID sourceFeatureId, Authentication authentication) {
        final String username = username(authentication);
        return CompletableFuture.supplyAsync(() -> {
            PlayerCharacter c = accessGuard.require(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(
                    characterFormService.startTransformation(c, monsterId, sourceFeatureId), "Превращение"));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "end transformation" в рамках бизнес-логики API.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/transformation/end")
    @Operation(summary = "End the character's active transformation")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> endTransformation(
            @PathVariable UUID characterId, Authentication authentication) {
        final String username = username(authentication);
        return CompletableFuture.supplyAsync(() -> {
            accessGuard.require(characterId, username);
            characterFormService.endTransformation(characterId);
            return ResponseEntity.ok(ApiResponse.<Void>ok(null, "Превращение завершено"));
        }, controllerTaskExecutor);
    }

    // ── Companions & tactical snapshot ──────────────────────────────────────

    /**
     * Создает результат операции "create companion" в рамках бизнес-логики API.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param monsterId идентификатор monster, используемый для выбора нужного бизнес-объекта
     * @param sourceFeatureId идентификатор source feature, используемый для выбора нужного бизнес-объекта
     * @param name входящее значение name, используемое бизнес-сценарием
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
    /**
     * Выполняет операции "companions" в рамках бизнес-логики API.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/companions")
    @Operation(summary = "List a character's feature companions")
    public CompletableFuture<ResponseEntity<ApiResponse<List<CompanionResponse>>>> companions(
            @PathVariable UUID characterId, Authentication authentication) {
        final String username = username(authentication);
        return CompletableFuture.supplyAsync(() -> {
            PlayerCharacter c = accessGuard.require(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(featureCompanionService.listCompanions(c)));
        }, controllerTaskExecutor);
    }

    /**
     * Создает результат операции "create companion" в рамках бизнес-логики API.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param monsterId идентификатор monster, используемый для выбора нужного бизнес-объекта
     * @param sourceFeatureId идентификатор source feature, используемый для выбора нужного бизнес-объекта
     * @param name входящее значение name, используемое бизнес-сценарием
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/companions")
    @Operation(summary = "Create a feature companion")
    public CompletableFuture<ResponseEntity<ApiResponse<CompanionResponse>>> createCompanion(
            @PathVariable UUID characterId, @RequestParam(required = false) UUID monsterId,
            @RequestParam(required = false) UUID sourceFeatureId, @RequestParam(required = false) String name,
            Authentication authentication) {
        final String username = username(authentication);
        return CompletableFuture.supplyAsync(() -> {
            PlayerCharacter c = accessGuard.require(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(
                    featureCompanionService.createCompanion(c, monsterId, sourceFeatureId, name), "Спутник создан"));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "dismiss companion" в рамках бизнес-логики API.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param companionId идентификатор companion, используемый для выбора нужного бизнес-объекта
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/companions/{companionId}/dismiss")
    @Operation(summary = "Dismiss a feature companion")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> dismissCompanion(
            @PathVariable UUID characterId, @PathVariable UUID companionId, Authentication authentication) {
        final String username = username(authentication);
        return CompletableFuture.supplyAsync(() -> {
            accessGuard.require(characterId, username);
            featureCompanionService.dismissCompanion(companionId);
            return ResponseEntity.ok(ApiResponse.<Void>ok(null, "Спутник отпущен"));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "tactical snapshot" в рамках бизнес-логики API.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/tactical-snapshot")
    @Operation(summary = "Tactical projection (active transformation + companions) for the frontend/map-service")
    public CompletableFuture<ResponseEntity<ApiResponse<TacticalSnapshot>>> tacticalSnapshot(
            @PathVariable UUID characterId, Authentication authentication) {
        final String username = username(authentication);
        return CompletableFuture.supplyAsync(() -> {
            PlayerCharacter c = accessGuard.require(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(featureCompanionService.tacticalSnapshot(c)));
        }, controllerTaskExecutor);
    }

    private static String username(Authentication authentication) {
        return authentication != null ? authentication.getName() : null;
    }
}
