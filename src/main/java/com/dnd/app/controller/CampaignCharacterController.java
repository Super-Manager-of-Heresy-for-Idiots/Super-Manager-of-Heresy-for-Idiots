package com.dnd.app.controller;

import com.dnd.app.dto.request.*;
import com.dnd.app.dto.response.*;
import com.dnd.app.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/characters")
@RequiredArgsConstructor
@Tag(name = "Campaign Characters", description = "Character management within campaigns")
public class CampaignCharacterController {

    private final CharacterService characterService;
    private final CharacterWizardService characterWizardService;
    private final ItemInstanceService itemInstanceService;
    private final CharacterEffectService characterEffectService;
    private final WalletService walletService;
    private final CharacterResourceService characterResourceService;

    // --- Full character creation (wizard) ---

    @PostMapping("/full")
    @Operation(summary = "Create a full 5e character via wizard")
    public ResponseEntity<ApiResponse<CharacterResponse>> createFullCharacter(
            @PathVariable UUID campaignId,
            @Valid @RequestBody CreateFullCharacterRequest request, Authentication auth) {
        CharacterResponse character = characterWizardService.createFullCharacter(campaignId, request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(character, "Персонаж создан"));
    }

    // --- Clone from template ---

    @PostMapping("/from-template/{templateId}")
    @Operation(summary = "Clone a vanilla template character into this campaign")
    public ResponseEntity<ApiResponse<CharacterResponse>> cloneFromTemplate(
            @PathVariable UUID campaignId,
            @PathVariable UUID templateId, Authentication auth) {
        CharacterResponse character = characterService.cloneCharacterToCampaign(campaignId, templateId, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(character, "Персонаж создан из шаблона"));
    }

    // --- Character CRUD ---

    @PostMapping
    @Operation(summary = "Create a character in campaign")
    public ResponseEntity<ApiResponse<CharacterResponse>> createCharacter(
            @PathVariable UUID campaignId,
            @Valid @RequestBody CreateCharacterRequest request, Authentication auth) {
        CharacterResponse character = characterService.createCharacter(campaignId, request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(character, "Персонаж создан"));
    }

    @GetMapping
    @Operation(summary = "List characters in campaign")
    public ResponseEntity<ApiResponse<List<CharacterResponse>>> listCharacters(
            @PathVariable UUID campaignId, Authentication auth) {
        List<CharacterResponse> characters = characterService.listCharacters(campaignId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(characters));
    }

    @GetMapping("/{characterId}")
    @Operation(summary = "Get character by ID")
    public ResponseEntity<ApiResponse<CharacterResponse>> getCharacter(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId, Authentication auth) {
        characterService.enforceCharacterInCampaign(characterId, campaignId);
        CharacterResponse character = characterService.getCharacterById(characterId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(character));
    }

    @PutMapping("/{characterId}")
    @Operation(summary = "Update character")
    public ResponseEntity<ApiResponse<CharacterResponse>> updateCharacter(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId,
            @Valid @RequestBody UpdateCharacterRequest request, Authentication auth) {
        characterService.enforceCharacterInCampaign(characterId, campaignId);
        CharacterResponse character = characterService.updateCharacter(characterId, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(character, "Персонаж обновлен"));
    }

    @DeleteMapping("/{characterId}")
    @Operation(summary = "Delete character")
    public ResponseEntity<ApiResponse<Void>> deleteCharacter(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId, Authentication auth) {
        characterService.enforceCharacterInCampaign(characterId, campaignId);
        characterService.deleteCharacter(characterId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Персонаж удален"));
    }

    // --- Stats ---

    @GetMapping("/{characterId}/stats")
    @Operation(summary = "Get character stats")
    public ResponseEntity<ApiResponse<List<CharacterStatResponse>>> getStats(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId, Authentication auth) {
        characterService.enforceCharacterInCampaign(characterId, campaignId);
        List<CharacterStatResponse> stats = characterService.getStats(characterId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    @PutMapping("/{characterId}/stats/{statId}")
    @Operation(summary = "Update character stat")
    public ResponseEntity<ApiResponse<CharacterStatResponse>> updateStat(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId,
            @PathVariable UUID statId,
            @Valid @RequestBody UpdateStatRequest request, Authentication auth) {
        characterService.enforceCharacterInCampaign(characterId, campaignId);
        CharacterStatResponse stat = characterService.updateStatValue(characterId, statId, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(stat, "Характеристика обновлена"));
    }

    // --- Inventory ---

    @GetMapping("/{characterId}/inventory")
    @Operation(summary = "Get character inventory (all items)")
    public ResponseEntity<ApiResponse<List<ItemInstanceResponse>>> getInventory(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId, Authentication auth) {
        characterService.enforceCharacterInCampaign(characterId, campaignId);
        List<ItemInstanceResponse> items = itemInstanceService.getInventory(characterId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @GetMapping("/{characterId}/inventory/equipped")
    @Operation(summary = "Get equipped items")
    public ResponseEntity<ApiResponse<List<ItemInstanceResponse>>> getEquippedItems(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId, Authentication auth) {
        characterService.enforceCharacterInCampaign(characterId, campaignId);
        List<ItemInstanceResponse> items = itemInstanceService.getEquippedItems(characterId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @GetMapping("/{characterId}/inventory/backpack")
    @Operation(summary = "Get backpack items")
    public ResponseEntity<ApiResponse<List<ItemInstanceResponse>>> getBackpackItems(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId, Authentication auth) {
        characterService.enforceCharacterInCampaign(characterId, campaignId);
        List<ItemInstanceResponse> items = itemInstanceService.getBackpackItems(characterId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(items));
    }

    @PostMapping("/{characterId}/inventory")
    @Operation(summary = "Grant item to character (GM only)")
    public ResponseEntity<ApiResponse<ItemInstanceResponse>> grantItem(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId,
            @Valid @RequestBody GrantItemRequest request, Authentication auth) {
        ItemInstanceResponse response = itemInstanceService.grantItem(campaignId, characterId, request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Item granted"));
    }

    @PostMapping("/{characterId}/inventory/{instanceId}/equip")
    @Operation(summary = "Equip an item")
    public ResponseEntity<ApiResponse<ItemInstanceResponse>> equipItem(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId,
            @PathVariable UUID instanceId,
            @Valid @RequestBody EquipItemRequest request, Authentication auth) {
        characterService.enforceCharacterInCampaign(characterId, campaignId);
        ItemInstanceResponse response = itemInstanceService.equipItem(characterId, instanceId, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Item equipped"));
    }

    @PostMapping("/{characterId}/inventory/{instanceId}/unequip")
    @Operation(summary = "Unequip an item")
    public ResponseEntity<ApiResponse<ItemInstanceResponse>> unequipItem(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId,
            @PathVariable UUID instanceId, Authentication auth) {
        characterService.enforceCharacterInCampaign(characterId, campaignId);
        ItemInstanceResponse response = itemInstanceService.unequipItem(characterId, instanceId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Item unequipped"));
    }

    @DeleteMapping("/{characterId}/inventory/{instanceId}")
    @Operation(summary = "Remove item from character (GM only)")
    public ResponseEntity<ApiResponse<Void>> removeItem(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId,
            @PathVariable UUID instanceId, Authentication auth) {
        itemInstanceService.removeItem(campaignId, characterId, instanceId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Item removed"));
    }

    @PostMapping("/{fromCharId}/inventory/{instanceId}/transfer")
    @Operation(summary = "Transfer item to another character")
    public ResponseEntity<ApiResponse<ItemInstanceResponse>> transferItem(
            @PathVariable UUID campaignId,
            @PathVariable UUID fromCharId,
            @PathVariable UUID instanceId,
            @Valid @RequestBody TransferItemRequest request, Authentication auth) {
        ItemInstanceResponse response = itemInstanceService.transferItem(campaignId, fromCharId, instanceId, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Item transferred"));
    }

    @PutMapping("/{characterId}/inventory/{instanceId}/rename")
    @Operation(summary = "Rename an item")
    public ResponseEntity<ApiResponse<ItemInstanceResponse>> renameItem(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId,
            @PathVariable UUID instanceId,
            @Valid @RequestBody RenameItemRequest request, Authentication auth) {
        characterService.enforceCharacterInCampaign(characterId, campaignId);
        ItemInstanceResponse response = itemInstanceService.renameItem(characterId, instanceId, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Item renamed"));
    }

    // --- Effects ---

    @GetMapping("/{characterId}/effects")
    @Operation(summary = "Get active effects on character")
    public ResponseEntity<ApiResponse<List<CharacterActiveEffectResponse>>> getEffects(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId, Authentication auth) {
        characterService.enforceCharacterInCampaign(characterId, campaignId);
        List<CharacterActiveEffectResponse> effects = characterEffectService.getActiveEffects(characterId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(effects));
    }

    @PostMapping("/{characterId}/effects")
    @Operation(summary = "Apply buff/debuff to character (GM only)")
    public ResponseEntity<ApiResponse<CharacterActiveEffectResponse>> applyEffect(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId,
            @Valid @RequestBody ApplyEffectRequest request, Authentication auth) {
        CharacterActiveEffectResponse response = characterEffectService.applyEffect(campaignId, characterId, request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Effect applied"));
    }

    @DeleteMapping("/{characterId}/effects/{effectId}")
    @Operation(summary = "Remove effect from character (GM only)")
    public ResponseEntity<ApiResponse<Void>> removeEffect(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId,
            @PathVariable UUID effectId, Authentication auth) {
        characterEffectService.removeEffect(campaignId, characterId, effectId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Effect removed"));
    }

    @GetMapping("/{characterId}/ability-check/{statTypeId}")
    @Operation(summary = "Calculate ability check modifier")
    public ResponseEntity<ApiResponse<AbilityCheckResponse>> abilityCheck(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId,
            @PathVariable UUID statTypeId, Authentication auth) {
        characterService.enforceCharacterInCampaign(characterId, campaignId);
        AbilityCheckResponse response = characterEffectService.calculateAbilityCheckModifier(characterId, statTypeId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // --- Wallet ---

    @GetMapping("/{characterId}/wallet")
    @Operation(summary = "Get character wallet")
    public ResponseEntity<ApiResponse<List<WalletEntryResponse>>> getWallet(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId, Authentication auth) {
        characterService.enforceCharacterInCampaign(characterId, campaignId);
        List<WalletEntryResponse> wallet = walletService.getWallet(characterId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(wallet));
    }

    @PostMapping("/{characterId}/wallet")
    @Operation(summary = "Modify currency amount")
    public ResponseEntity<ApiResponse<WalletEntryResponse>> modifyCurrency(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId,
            @Valid @RequestBody ModifyCurrencyRequest request, Authentication auth) {
        characterService.enforceCharacterInCampaign(characterId, campaignId);
        WalletEntryResponse response = walletService.modifyCurrency(characterId, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Currency updated"));
    }

    @GetMapping("/{characterId}/wallet/history")
    @Operation(summary = "Get character wallet transaction history")
    public ResponseEntity<ApiResponse<PageResponse<WalletHistoryEntryResponse>>> getWalletHistory(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId,
            Pageable pageable, Authentication auth) {
        characterService.enforceCharacterInCampaign(characterId, campaignId);
        PageResponse<WalletHistoryEntryResponse> history =
                walletService.getWalletHistory(characterId, pageable, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    // --- Resources ---

    @GetMapping("/{characterId}/resources")
    @Operation(summary = "Get character resources")
    public ResponseEntity<ApiResponse<List<ResourceResponse>>> getResources(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId, Authentication auth) {
        characterService.enforceCharacterInCampaign(characterId, campaignId);
        List<ResourceResponse> resources = characterResourceService.getResources(characterId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(resources));
    }

    @PostMapping("/{characterId}/resources")
    @Operation(summary = "Modify resource value")
    public ResponseEntity<ApiResponse<ResourceResponse>> modifyResource(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId,
            @Valid @RequestBody ModifyResourceRequest request, Authentication auth) {
        characterService.enforceCharacterInCampaign(characterId, campaignId);
        ResourceResponse response = characterResourceService.modifyResource(characterId, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Resource updated"));
    }

    // --- HP ---

    @PostMapping("/{characterId}/hp")
    @Operation(summary = "Modify character HP")
    public ResponseEntity<ApiResponse<CharacterResponse>> modifyHp(
            @PathVariable UUID campaignId,
            @PathVariable UUID characterId,
            @Valid @RequestBody ModifyHpRequest request, Authentication auth) {
        CharacterResponse response = characterService.modifyHp(campaignId, characterId, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "HP modified"));
    }
}
