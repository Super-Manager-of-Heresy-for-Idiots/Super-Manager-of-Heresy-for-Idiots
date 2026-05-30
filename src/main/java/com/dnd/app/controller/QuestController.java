package com.dnd.app.controller;

import com.dnd.app.dto.request.*;
import com.dnd.app.dto.response.*;
import com.dnd.app.service.QuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/quests")
@RequiredArgsConstructor
@Tag(name = "Quests", description = "Campaign quest management")
public class QuestController {

    private final QuestService questService;

    @PostMapping
    @Operation(summary = "Create quest (GM only)")
    public ResponseEntity<ApiResponse<QuestResponse>> createQuest(
            @PathVariable UUID campaignId,
            @Valid @RequestBody CreateQuestRequest request, Authentication auth) {
        QuestResponse response = questService.createQuest(campaignId, request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Quest created"));
    }

    @GetMapping
    @Operation(summary = "List quests")
    public ResponseEntity<ApiResponse<List<QuestResponse>>> listQuests(
            @PathVariable UUID campaignId, Authentication auth) {
        List<QuestResponse> quests = questService.listQuests(campaignId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(quests));
    }

    @GetMapping("/{questId}")
    @Operation(summary = "Get quest details")
    public ResponseEntity<ApiResponse<QuestResponse>> getQuest(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId, Authentication auth) {
        QuestResponse response = questService.getQuest(questId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{questId}")
    @Operation(summary = "Update quest (GM only)")
    public ResponseEntity<ApiResponse<QuestResponse>> updateQuest(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId,
            @Valid @RequestBody UpdateQuestRequest request, Authentication auth) {
        QuestResponse response = questService.updateQuest(questId, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Quest updated"));
    }

    @DeleteMapping("/{questId}")
    @Operation(summary = "Delete quest (GM only)")
    public ResponseEntity<ApiResponse<Void>> deleteQuest(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId, Authentication auth) {
        questService.deleteQuest(questId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Quest deleted"));
    }

    // --- Quest rewards ---

    @GetMapping("/{questId}/rewards")
    @Operation(summary = "List quest rewards")
    public ResponseEntity<ApiResponse<List<QuestRewardResponse>>> listRewards(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId, Authentication auth) {
        List<QuestRewardResponse> rewards = questService.listRewards(questId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(rewards));
    }

    @PostMapping("/{questId}/rewards")
    @Operation(summary = "Add reward to quest (GM only)")
    public ResponseEntity<ApiResponse<QuestRewardResponse>> addReward(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId,
            @Valid @RequestBody com.dnd.app.dto.request.CreateQuestRewardRequest request, Authentication auth) {
        QuestRewardResponse response = questService.addReward(questId, request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Reward added"));
    }

    @DeleteMapping("/{questId}/rewards/{rewardId}")
    @Operation(summary = "Delete quest reward (GM only)")
    public ResponseEntity<ApiResponse<Void>> deleteReward(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId,
            @PathVariable UUID rewardId, Authentication auth) {
        questService.deleteReward(rewardId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Reward deleted"));
    }

    // --- Notes ---

    @PostMapping("/{questId}/notes")
    @Operation(summary = "Add note to quest")
    public ResponseEntity<ApiResponse<NoteResponse>> addNote(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId,
            @Valid @RequestBody CreateNoteRequest request, Authentication auth) {
        NoteResponse response = questService.addNote(questId, request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Note added"));
    }

    @PutMapping("/{questId}/notes/{noteId}")
    @Operation(summary = "Update quest note")
    public ResponseEntity<ApiResponse<NoteResponse>> updateNote(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId,
            @PathVariable UUID noteId,
            @Valid @RequestBody UpdateNoteRequest request, Authentication auth) {
        NoteResponse response = questService.updateNote(noteId, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Note updated"));
    }

    @DeleteMapping("/{questId}/notes/{noteId}")
    @Operation(summary = "Delete quest note")
    public ResponseEntity<ApiResponse<Void>> deleteNote(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId,
            @PathVariable UUID noteId, Authentication auth) {
        questService.deleteNote(noteId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Note deleted"));
    }

    @PostMapping("/{questId}/npcs/{npcId}")
    @Operation(summary = "Link NPC to quest (GM only)")
    public ResponseEntity<ApiResponse<Void>> linkNpc(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId,
            @PathVariable UUID npcId, Authentication auth) {
        questService.linkNpc(questId, npcId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "NPC linked to quest"));
    }

    @DeleteMapping("/{questId}/npcs/{npcId}")
    @Operation(summary = "Unlink NPC from quest (GM only)")
    public ResponseEntity<ApiResponse<Void>> unlinkNpc(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId,
            @PathVariable UUID npcId, Authentication auth) {
        questService.unlinkNpc(questId, npcId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "NPC unlinked from quest"));
    }

    @PostMapping("/{questId}/locations/{locationId}")
    @Operation(summary = "Link location to quest (GM only)")
    public ResponseEntity<ApiResponse<Void>> linkLocation(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId,
            @PathVariable UUID locationId, Authentication auth) {
        questService.linkLocation(questId, locationId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Location linked to quest"));
    }

    @DeleteMapping("/{questId}/locations/{locationId}")
    @Operation(summary = "Unlink location from quest (GM only)")
    public ResponseEntity<ApiResponse<Void>> unlinkLocation(
            @PathVariable UUID campaignId,
            @PathVariable UUID questId,
            @PathVariable UUID locationId, Authentication auth) {
        questService.unlinkLocation(questId, locationId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Location unlinked from quest"));
    }
}
