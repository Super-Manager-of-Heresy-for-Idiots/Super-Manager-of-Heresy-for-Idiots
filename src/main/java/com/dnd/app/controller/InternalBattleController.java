package com.dnd.app.controller;

import com.dnd.app.dto.request.MovementRequest;
import com.dnd.app.dto.response.BattleAccessResponse;
import com.dnd.app.dto.response.CombatantReferenceResponse;
import com.dnd.app.dto.response.MovementContextResponse;
import com.dnd.app.dto.response.MovementResultResponse;
import com.dnd.app.service.BattleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Service-to-service contracts that let map-service link tactical map tokens to core battle
 * combatants without reaching into the core database or duplicating combat permission rules.
 * Combat authority stays in core BE; these are read-only projections and carry no map/grid state.
 *
 * <p>Secured by a shared service API key (see {@code com.dnd.app.security.InternalApiKeyFilter}),
 * not the user JWT: callers must present {@code X-Internal-Api-Key}.
 */
@RestController
@RequestMapping("/api/internal/campaigns/{campaignId}/battles/{battleId}")
@RequiredArgsConstructor
@Tag(name = "Internal Battle", description = "Service-to-service battle access and combatant references")
public class InternalBattleController {

    private final BattleService battleService;

    @GetMapping("/access")
    @Operation(summary = "What a user may do in this battle (for map-service token control)")
    public ResponseEntity<BattleAccessResponse> getBattleAccess(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @RequestParam UUID userId) {
        BattleAccessResponse data = battleService.getBattleAccess(campaignId, battleId, userId);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/combatants/{combatantId}/reference")
    @Operation(summary = "Safe combatant identity to create a token-combat link from")
    public ResponseEntity<CombatantReferenceResponse> getCombatantReference(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @PathVariable UUID combatantId) {
        CombatantReferenceResponse data = battleService.getCombatantReference(campaignId, battleId, combatantId);
        return ResponseEntity.ok(data);
    }

    @PostMapping("/movement")
    @Operation(summary = "Validate and commit a combatant's movement budget for the current turn")
    public ResponseEntity<MovementResultResponse> applyMovement(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @Valid @RequestBody MovementRequest request) {
        MovementResultResponse data = battleService.applyMovement(campaignId, battleId, request);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/movement-context")
    @Operation(summary = "Active combatant plus every combatant's speed and spent movement (for FE previews)")
    public ResponseEntity<MovementContextResponse> movementContext(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId) {
        MovementContextResponse data = battleService.movementContext(campaignId, battleId);
        return ResponseEntity.ok(data);
    }
}
