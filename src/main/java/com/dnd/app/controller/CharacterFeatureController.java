package com.dnd.app.controller;

import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.featurerule.ActiveEffectResponse;
import com.dnd.app.dto.featurerule.AvailableFeatureAction;
import com.dnd.app.dto.featurerule.CharacterFeatureResourceResponse;
import com.dnd.app.dto.featurerule.FeatureApplyRequest;
import com.dnd.app.dto.featurerule.FeatureApplyResult;
import com.dnd.app.dto.featurerule.FeatureChoiceGroupResponse;
import com.dnd.app.dto.featurerule.FeatureExecutionPlan;
import com.dnd.app.dto.featurerule.FeatureSpellCastResult;
import com.dnd.app.dto.featurerule.FeatureSpellGrantResponse;
import com.dnd.app.dto.featurerule.FeatureUseRequest;
import com.dnd.app.dto.featurerule.FeatureUseResult;
import com.dnd.app.dto.featurerule.PendingPromptResponse;
import com.dnd.app.dto.featurerule.RestResourcePreview;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.service.ActiveEffectQueryService;
import com.dnd.app.service.CampaignService;
import com.dnd.app.service.CharacterFeatureChoiceService;
import com.dnd.app.service.CombatFeatureExecutionService;
import com.dnd.app.service.EffectExpirationService;
import com.dnd.app.service.FeatureActionService;
import com.dnd.app.service.FeatureResourceService;
import com.dnd.app.service.FeatureSpellGrantService;
import com.dnd.app.service.FeatureUseService;
import com.dnd.app.service.PendingGameplayPromptService;
import com.dnd.app.service.RestFeatureRuntimeService;
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
 * Player/GM-facing feature-rules runtime for a character: resource counters, spending, GM adjustment,
 * and rest recovery (Stage 5). All endpoints are no-ops in effect unless {@code app.feature-rules} is
 * enabled (no resource rows exist otherwise). Access = character owner, campaign GM, or ADMIN.
 */
@RestController
@RequestMapping("/api/characters/{characterId}/features")
@RequiredArgsConstructor
@Tag(name = "Character Feature Runtime", description = "Feature resources and rest for a character")
public class CharacterFeatureController {

    private final FeatureResourceService featureResourceService;
    private final RestFeatureRuntimeService restFeatureRuntimeService;
    private final FeatureActionService featureActionService;
    private final FeatureUseService featureUseService;
    private final ActiveEffectQueryService activeEffectQueryService;
    private final EffectExpirationService effectExpirationService;
    private final CombatFeatureExecutionService combatFeatureExecutionService;
    private final FeatureSpellGrantService featureSpellGrantService;
    private final PendingGameplayPromptService pendingGameplayPromptService;
    private final CharacterFeatureChoiceService featureChoiceService;
    private final PlayerCharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final Executor controllerTaskExecutor;

    @GetMapping("/resources")
    @Operation(summary = "List a character's feature resource counters")
    public CompletableFuture<ResponseEntity<ApiResponse<List<CharacterFeatureResourceResponse>>>> resources(
            @PathVariable UUID characterId, Authentication authentication) {
        final String username = usernameOf(authentication);
        return CompletableFuture.supplyAsync(() -> {
            requireCharacter(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(featureResourceService.listResponses(characterId)));
        }, controllerTaskExecutor);
    }

    @PostMapping("/resources/{resourceId}/spend")
    @Operation(summary = "Spend from a feature resource")
    public CompletableFuture<ResponseEntity<ApiResponse<CharacterFeatureResourceResponse>>> spend(
            @PathVariable UUID characterId, @PathVariable UUID resourceId,
            @RequestParam(defaultValue = "1") int amount, Authentication authentication) {
        final String username = usernameOf(authentication);
        return CompletableFuture.supplyAsync(() -> {
            requireCharacter(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(
                    featureResourceService.toResponse(featureResourceService.spend(characterId, resourceId, amount))));
        }, controllerTaskExecutor);
    }

    @PostMapping("/resources/{resourceId}/adjust")
    @Operation(summary = "GM/manual set of a feature resource's current value")
    public CompletableFuture<ResponseEntity<ApiResponse<CharacterFeatureResourceResponse>>> adjust(
            @PathVariable UUID characterId, @PathVariable UUID resourceId,
            @RequestParam int value, Authentication authentication) {
        final String username = usernameOf(authentication);
        return CompletableFuture.supplyAsync(() -> {
            requireCharacter(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(
                    featureResourceService.toResponse(featureResourceService.setValue(characterId, resourceId, value))));
        }, controllerTaskExecutor);
    }

    @GetMapping("/actions")
    @Operation(summary = "List feature actions a character can currently use (with cost/availability)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<AvailableFeatureAction>>>> actions(
            @PathVariable UUID characterId, Authentication authentication) {
        final String username = usernameOf(authentication);
        return CompletableFuture.supplyAsync(() -> {
            PlayerCharacter character = requireCharacter(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(featureActionService.listAvailableActions(character)));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{featureId}/use")
    @Operation(summary = "Use a feature: spend its action/resource cost and record it")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatureUseResult>>> use(
            @PathVariable UUID characterId, @PathVariable UUID featureId,
            @RequestBody(required = false) FeatureUseRequest request, Authentication authentication) {
        final String username = usernameOf(authentication);
        return CompletableFuture.supplyAsync(() -> {
            PlayerCharacter character = requireCharacter(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(
                    featureUseService.use(character, featureId, request), "Умение использовано"));
        }, controllerTaskExecutor);
    }

    @GetMapping("/{featureId}/plan")
    @Operation(summary = "Compute a feature's structured combat resolution (damage/DC/save/attack)")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatureExecutionPlan>>> plan(
            @PathVariable UUID characterId, @PathVariable UUID featureId, Authentication authentication) {
        final String username = usernameOf(authentication);
        return CompletableFuture.supplyAsync(() -> {
            PlayerCharacter actor = requireCharacter(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(combatFeatureExecutionService.plan(actor, featureId)));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{featureId}/apply")
    @Operation(summary = "Apply a rolled feature outcome (damage/healing) to a target character")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatureApplyResult>>> apply(
            @PathVariable UUID characterId, @PathVariable UUID featureId,
            @RequestBody FeatureApplyRequest request, Authentication authentication) {
        final String username = usernameOf(authentication);
        return CompletableFuture.supplyAsync(() -> {
            PlayerCharacter actor = requireCharacter(characterId, username);
            PlayerCharacter target = requireCharacter(request.getTargetCharacterId(), username);
            return ResponseEntity.ok(ApiResponse.ok(combatFeatureExecutionService.applyToTarget(
                    actor, featureId, target, request.getDamage(), request.getHealing()), "Применено"));
        }, controllerTaskExecutor);
    }

    @GetMapping("/pending-prompts")
    @Operation(summary = "List a character's pending gameplay prompts (reactions/optional triggers)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<PendingPromptResponse>>>> pendingPrompts(
            @PathVariable UUID characterId, Authentication authentication) {
        final String username = usernameOf(authentication);
        return CompletableFuture.supplyAsync(() -> {
            requireCharacter(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(pendingGameplayPromptService.listPending(characterId)));
        }, controllerTaskExecutor);
    }

    @PostMapping("/pending-prompts/{promptId}/resolve")
    @Operation(summary = "Resolve a pending prompt (spend reaction/resource and apply)")
    public CompletableFuture<ResponseEntity<ApiResponse<PendingPromptResponse>>> resolvePrompt(
            @PathVariable UUID characterId, @PathVariable UUID promptId, Authentication authentication) {
        final String username = usernameOf(authentication);
        return CompletableFuture.supplyAsync(() -> {
            requireCharacter(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(
                    pendingGameplayPromptService.resolve(characterId, promptId), "Реакция применена"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/pending-prompts/{promptId}/decline")
    @Operation(summary = "Decline a pending prompt")
    public CompletableFuture<ResponseEntity<ApiResponse<PendingPromptResponse>>> declinePrompt(
            @PathVariable UUID characterId, @PathVariable UUID promptId, Authentication authentication) {
        final String username = usernameOf(authentication);
        return CompletableFuture.supplyAsync(() -> {
            requireCharacter(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(
                    pendingGameplayPromptService.decline(characterId, promptId), "Отклонено"));
        }, controllerTaskExecutor);
    }

    @GetMapping("/choices")
    @Operation(summary = "List a character's feature choices (Fighting Style, Expertise, Metamagic…)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<FeatureChoiceGroupResponse>>>> choices(
            @PathVariable UUID characterId, Authentication authentication) {
        final String username = usernameOf(authentication);
        return CompletableFuture.supplyAsync(() -> {
            requireCharacter(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(featureChoiceService.list(characterId)));
        }, controllerTaskExecutor);
    }

    @PostMapping("/choices/{groupId}")
    @Operation(summary = "Record a feature choice selection (applies skills; other types recorded)")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatureChoiceGroupResponse>>> chooseFeature(
            @PathVariable UUID characterId, @PathVariable UUID groupId,
            @RequestParam String optionType, @RequestParam(required = false) UUID targetEntityId,
            Authentication authentication) {
        final String username = usernameOf(authentication);
        return CompletableFuture.supplyAsync(() -> {
            requireCharacter(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(
                    featureChoiceService.choose(characterId, groupId, optionType, targetEntityId), "Выбор сохранён"));
        }, controllerTaskExecutor);
    }

    @DeleteMapping("/choices/{choiceId}")
    @Operation(summary = "Remove a feature choice selection")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> removeChoice(
            @PathVariable UUID characterId, @PathVariable UUID choiceId, Authentication authentication) {
        final String username = usernameOf(authentication);
        return CompletableFuture.supplyAsync(() -> {
            requireCharacter(characterId, username);
            featureChoiceService.unchoose(characterId, choiceId);
            return ResponseEntity.ok(ApiResponse.<Void>ok(null, "Выбор удалён"));
        }, controllerTaskExecutor);
    }

    @GetMapping("/spells")
    @Operation(summary = "List spells a character's features grant (with prepared/known/free-cast flags)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<FeatureSpellGrantResponse>>>> spells(
            @PathVariable UUID characterId, Authentication authentication) {
        final String username = usernameOf(authentication);
        return CompletableFuture.supplyAsync(() -> {
            PlayerCharacter character = requireCharacter(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(featureSpellGrantService.listGrantedSpells(character)));
        }, controllerTaskExecutor);
    }

    @PostMapping("/spell-grants/{grantId}/cast")
    @Operation(summary = "Cast a spell via a feature grant (free cast / resource spend)")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatureSpellCastResult>>> castViaFeature(
            @PathVariable UUID characterId, @PathVariable UUID grantId, Authentication authentication) {
        final String username = usernameOf(authentication);
        return CompletableFuture.supplyAsync(() -> {
            PlayerCharacter character = requireCharacter(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(
                    featureSpellGrantService.castViaFeature(character, grantId), "Каст выполнен"));
        }, controllerTaskExecutor);
    }

    @GetMapping("/effects")
    @Operation(summary = "List a character's active feature effects with modifiers")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ActiveEffectResponse>>>> effects(
            @PathVariable UUID characterId, Authentication authentication) {
        final String username = usernameOf(authentication);
        return CompletableFuture.supplyAsync(() -> {
            requireCharacter(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(activeEffectQueryService.listActive(characterId)));
        }, controllerTaskExecutor);
    }

    @PostMapping("/effects/{effectId}/end")
    @Operation(summary = "GM: end an active feature effect")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> endEffect(
            @PathVariable UUID characterId, @PathVariable UUID effectId, Authentication authentication) {
        final String username = usernameOf(authentication);
        return CompletableFuture.supplyAsync(() -> {
            requireCharacter(characterId, username);
            effectExpirationService.gmEnd(effectId);
            return ResponseEntity.ok(ApiResponse.<Void>ok(null, "Эффект завершён"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/effects/{effectId}/rounds")
    @Operation(summary = "GM: set the remaining rounds of an active feature effect")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> setEffectRounds(
            @PathVariable UUID characterId, @PathVariable UUID effectId,
            @RequestParam int value, Authentication authentication) {
        final String username = usernameOf(authentication);
        return CompletableFuture.supplyAsync(() -> {
            requireCharacter(characterId, username);
            effectExpirationService.gmSetRounds(effectId, value);
            return ResponseEntity.ok(ApiResponse.<Void>ok(null, "Длительность обновлена"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/rest/preview")
    @Operation(summary = "Preview what a rest would restore for feature resources")
    public CompletableFuture<ResponseEntity<ApiResponse<List<RestResourcePreview>>>> restPreview(
            @PathVariable UUID characterId, @RequestParam String restType, Authentication authentication) {
        final String username = usernameOf(authentication);
        return CompletableFuture.supplyAsync(() -> {
            PlayerCharacter character = requireCharacter(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(restFeatureRuntimeService.preview(character, restType)));
        }, controllerTaskExecutor);
    }

    @PostMapping("/rest/complete")
    @Operation(summary = "Apply a rest's feature-resource recovery")
    public CompletableFuture<ResponseEntity<ApiResponse<List<RestResourcePreview>>>> restComplete(
            @PathVariable UUID characterId, @RequestParam String restType, Authentication authentication) {
        final String username = usernameOf(authentication);
        return CompletableFuture.supplyAsync(() -> {
            PlayerCharacter character = requireCharacter(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(
                    restFeatureRuntimeService.complete(character, restType), "Отдых применён"));
        }, controllerTaskExecutor);
    }

    private static String usernameOf(Authentication authentication) {
        return authentication != null ? authentication.getName() : null;
    }

    private PlayerCharacter requireCharacter(UUID characterId, String username) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        boolean owner = character.getOwner() != null && character.getOwner().getId().equals(user.getId());
        boolean gm = character.getCampaign() != null
                && campaignService.isGmInCampaign(character.getCampaign().getId(), user.getId());
        if (!owner && !gm && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Нет прав на этого персонажа");
        }
        return character;
    }
}
