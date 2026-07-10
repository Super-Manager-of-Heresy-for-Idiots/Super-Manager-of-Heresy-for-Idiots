package com.dnd.app.controller;

import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.User;
import com.dnd.app.dto.featurerule.FeatureApplyRequest;
import com.dnd.app.dto.featurerule.FeatureApplyResult;
import com.dnd.app.dto.featurerule.FeatureExecutionPlan;
import com.dnd.app.dto.featurerule.SpellCastRequest;
import com.dnd.app.dto.featurerule.SpellCastResult;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.CharacterKnownSpellResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.security.CharacterAccessGuard;
import com.dnd.app.service.CharacterSpellbookService;
import com.dnd.app.service.SpellCastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Класс CharacterSpellbookController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequestMapping("/api/characters/{characterId}/spellbook")
@RequiredArgsConstructor
@Tag(name = "Character Spellbook", description = "Record/forget a character's known spells")
public class CharacterSpellbookController {

    private final CharacterSpellbookService spellbookService;
    private final SpellCastService spellCastService;
    private final UserRepository userRepository;
    private final CharacterAccessGuard accessGuard;
    private final Executor controllerTaskExecutor;

    /**
     * Возвращает список для операции "list" в рамках бизнес-логики API.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping
    @Operation(summary = "List a character's recorded spells")
    public CompletableFuture<ResponseEntity<ApiResponse<List<CharacterKnownSpellResponse>>>> list(
            @PathVariable UUID characterId, Authentication authentication) {
        final String username = username(authentication);
        return CompletableFuture.supplyAsync(() -> {
            accessGuard.require(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(spellbookService.list(characterId)));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "learn" в рамках бизнес-логики API.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param spellId идентификатор spell, используемый для выбора нужного бизнес-объекта
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping
    @Operation(summary = "Record (learn) a spell into the character's spellbook")
    public CompletableFuture<ResponseEntity<ApiResponse<CharacterKnownSpellResponse>>> learn(
            @PathVariable UUID characterId, @RequestParam UUID spellId, Authentication authentication) {
        final String username = username(authentication);
        return CompletableFuture.supplyAsync(() -> {
            accessGuard.require(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(spellbookService.learn(characterId, spellId), "Заклинание записано"));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "forget" в рамках бизнес-логики API.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param spellId идентификатор spell, используемый для выбора нужного бизнес-объекта
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @DeleteMapping("/{spellId}")
    @Operation(summary = "Forget (remove) a spell from the character's spellbook")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> forget(
            @PathVariable UUID characterId, @PathVariable UUID spellId, Authentication authentication) {
        final String username = username(authentication);
        return CompletableFuture.supplyAsync(() -> {
            accessGuard.require(characterId, username);
            spellbookService.forget(characterId, spellId);
            return ResponseEntity.ok(ApiResponse.<Void>ok(null, "Заклинание удалено"));
        }, controllerTaskExecutor);
    }

    // ── Cast through the feature-rules runtime (S2 spell-stack absorption) ──

    /**
     * Выполняет операции "plan" в рамках бизнес-логики API.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param spellId идентификатор spell, используемый для выбора нужного бизнес-объекта
     * @param slotLevel входящее значение slot level, используемое бизнес-сценарием
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/{spellId}/plan")
    @Operation(summary = "Roll plan of a spell (damage dice, DC, healing) without spending anything")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatureExecutionPlan>>> plan(
            @PathVariable UUID characterId, @PathVariable UUID spellId,
            @RequestParam(required = false) Integer slotLevel, Authentication authentication) {
        final String username = username(authentication);
        return CompletableFuture.supplyAsync(() -> {
            PlayerCharacter caster = accessGuard.require(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(spellCastService.plan(caster, spellId, slotLevel)));
        }, controllerTaskExecutor);
    }

    /**
     * Применяет заклинание операции "cast" в рамках бизнес-логики API.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param spellId идентификатор spell, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/{spellId}/cast")
    @Operation(summary = "Cast a known spell: spend the combat action + slot, apply effects, return the roll plan")
    public CompletableFuture<ResponseEntity<ApiResponse<SpellCastResult>>> cast(
            @PathVariable UUID characterId, @PathVariable UUID spellId,
            @RequestBody(required = false) SpellCastRequest request, Authentication authentication) {
        final String username = username(authentication);
        return CompletableFuture.supplyAsync(() -> {
            PlayerCharacter caster = accessGuard.require(characterId, username);
            PlayerCharacter effectTarget = request != null && request.getTargetCharacterId() != null
                    ? accessGuard.require(request.getTargetCharacterId(), username)
                    : null;
            return ResponseEntity.ok(ApiResponse.ok(
                    spellCastService.cast(caster, spellId, request, effectTarget), "Заклинание использовано"));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "apply" в рамках бизнес-логики API.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param spellId идентификатор spell, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param authentication входящее значение authentication, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/{spellId}/apply")
    @Operation(summary = "Apply a rolled spell outcome (damage/healing) to a target character")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatureApplyResult>>> apply(
            @PathVariable UUID characterId, @PathVariable UUID spellId,
            @RequestBody FeatureApplyRequest request, Authentication authentication) {
        final String username = username(authentication);
        return CompletableFuture.supplyAsync(() -> {
            if (request == null || request.getTargetCharacterId() == null) {
                throw new BadRequestException("Не указана цель применения");
            }
            PlayerCharacter caster = accessGuard.require(characterId, username);
            PlayerCharacter target = accessGuard.require(request.getTargetCharacterId(), username);
            UUID campaignId = target.getCampaign() != null ? target.getCampaign().getId() : null;
            UUID actorUserId = userRepository.findByUsername(username).map(User::getId).orElse(null);
            return ResponseEntity.ok(ApiResponse.ok(spellCastService.applyToTarget(
                    caster, spellId, target, request.getDamage(), request.getHealing(),
                    request.getDamageTypeId(), campaignId, actorUserId), "Применено"));
        }, controllerTaskExecutor);
    }

    private static String username(Authentication authentication) {
        return authentication != null ? authentication.getName() : null;
    }
}
