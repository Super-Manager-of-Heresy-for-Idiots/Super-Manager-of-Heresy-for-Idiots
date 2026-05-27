package com.dnd.app.controller;

import com.dnd.app.dto.request.AddConditionModifierRequest;
import com.dnd.app.dto.request.ApplyConditionRequest;
import com.dnd.app.dto.request.CreateConditionRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.CharacterConditionResponse;
import com.dnd.app.dto.response.ConditionResponse;
import com.dnd.app.service.ConditionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conditions")
@RequiredArgsConstructor
public class ConditionController {

    private final ConditionService conditionService;

    @PostMapping
    public ResponseEntity<ApiResponse<ConditionResponse>> create(
            @Valid @RequestBody CreateConditionRequest request, Authentication auth) {
        ConditionResponse condition = conditionService.createCondition(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(condition, "Condition created"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConditionResponse>>> list(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(conditionService.listConditions(auth.getName())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConditionResponse>> get(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(conditionService.getCondition(id, auth.getName())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ConditionResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody CreateConditionRequest request, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(conditionService.updateCondition(id, request, auth.getName()), "Condition updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, Authentication auth) {
        conditionService.deleteCondition(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Condition deleted"));
    }

    @PostMapping("/{id}/modifiers")
    public ResponseEntity<ApiResponse<ConditionResponse>> addModifier(
            @PathVariable UUID id, @Valid @RequestBody AddConditionModifierRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(conditionService.addModifier(id, request, auth.getName()), "Modifier added"));
    }

    @DeleteMapping("/{condId}/modifiers/{modId}")
    public ResponseEntity<ApiResponse<Void>> deleteModifier(
            @PathVariable UUID condId, @PathVariable UUID modId, Authentication auth) {
        conditionService.deleteModifier(condId, modId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Modifier removed"));
    }

    @PostMapping("/apply/{characterId}")
    public ResponseEntity<ApiResponse<CharacterConditionResponse>> apply(
            @PathVariable UUID characterId, @Valid @RequestBody ApplyConditionRequest request, Authentication auth) {
        CharacterConditionResponse resp = conditionService.applyCondition(characterId, request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(resp, "Condition applied"));
    }

    @GetMapping("/character/{characterId}")
    public ResponseEntity<ApiResponse<List<CharacterConditionResponse>>> getActive(
            @PathVariable UUID characterId, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(conditionService.getActiveConditions(characterId, auth.getName())));
    }

    @DeleteMapping("/character/{characterId}/{charConditionId}")
    public ResponseEntity<ApiResponse<Void>> remove(
            @PathVariable UUID characterId, @PathVariable UUID charConditionId, Authentication auth) {
        conditionService.removeCondition(characterId, charConditionId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Condition removed"));
    }
}
