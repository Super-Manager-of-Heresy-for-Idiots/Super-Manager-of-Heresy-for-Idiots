package com.dnd.app.controller;

import com.dnd.app.dto.request.*;
import com.dnd.app.dto.response.*;
import com.dnd.app.service.NpcService;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/npcs")
@RequiredArgsConstructor
@Tag(name = "NPCs", description = "Campaign NPC management")
public class NpcController {

    private final NpcService npcService;
    private final Executor controllerTaskExecutor;

    @PostMapping
    @Operation(summary = "Create NPC (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<NpcResponse>>> createNpc(
            @PathVariable UUID campaignId,
            @Valid @RequestBody CreateNpcRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            NpcResponse response = npcService.createNpc(campaignId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "NPC created"));
        }, controllerTaskExecutor);
    }

    @GetMapping
    @Operation(summary = "List NPCs")
    public CompletableFuture<ResponseEntity<ApiResponse<List<NpcResponse>>>> listNpcs(
            @PathVariable UUID campaignId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<NpcResponse> npcs = npcService.listNpcs(campaignId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(npcs));
        }, controllerTaskExecutor);
    }

    @GetMapping("/{npcId}")
    @Operation(summary = "Get NPC details")
    public CompletableFuture<ResponseEntity<ApiResponse<NpcResponse>>> getNpc(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            NpcResponse response = npcService.getNpc(npcId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response));
        }, controllerTaskExecutor);
    }

    @PutMapping("/{npcId}")
    @Operation(summary = "Update NPC (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<NpcResponse>>> updateNpc(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId,
            @Valid @RequestBody UpdateNpcRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            NpcResponse response = npcService.updateNpc(npcId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "NPC updated"));
        }, controllerTaskExecutor);
    }

    @DeleteMapping("/{npcId}")
    @Operation(summary = "Delete NPC (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteNpc(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            npcService.deleteNpc(npcId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "NPC deleted"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{npcId}/toggle-visibility")
    @Operation(summary = "Toggle NPC visibility (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<NpcResponse>>> toggleVisibility(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            NpcResponse response = npcService.toggleVisibility(npcId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Visibility toggled"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{npcId}/notes")
    @Operation(summary = "Add note to NPC")
    public CompletableFuture<ResponseEntity<ApiResponse<NoteResponse>>> addNote(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId,
            @Valid @RequestBody CreateNoteRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            NoteResponse response = npcService.addNote(npcId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Note added"));
        }, controllerTaskExecutor);
    }

    @PutMapping("/{npcId}/notes/{noteId}")
    @Operation(summary = "Update note")
    public CompletableFuture<ResponseEntity<ApiResponse<NoteResponse>>> updateNote(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId,
            @PathVariable UUID noteId,
            @Valid @RequestBody UpdateNoteRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            NoteResponse response = npcService.updateNote(noteId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Note updated"));
        }, controllerTaskExecutor);
    }

    @DeleteMapping("/{npcId}/notes/{noteId}")
    @Operation(summary = "Delete note")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteNote(
            @PathVariable UUID campaignId,
            @PathVariable UUID npcId,
            @PathVariable UUID noteId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            npcService.deleteNote(noteId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Note deleted"));
        }, controllerTaskExecutor);
    }
}
