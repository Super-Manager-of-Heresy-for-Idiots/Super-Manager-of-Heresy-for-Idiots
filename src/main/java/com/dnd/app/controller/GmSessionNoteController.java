package com.dnd.app.controller;

import com.dnd.app.dto.request.*;
import com.dnd.app.dto.response.*;
import com.dnd.app.service.GmSessionNoteService;
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
@RequestMapping("/api/campaigns/{campaignId}/gm-notes")
@RequiredArgsConstructor
@Tag(name = "GM Session Notes", description = "GM-only session notes")
public class GmSessionNoteController {

    private final GmSessionNoteService gmSessionNoteService;

    @PostMapping
    @Operation(summary = "Create GM session note")
    public ResponseEntity<ApiResponse<GmSessionNoteResponse>> createNote(
            @PathVariable UUID campaignId,
            @Valid @RequestBody CreateGmNoteRequest request, Authentication auth) {
        GmSessionNoteResponse response = gmSessionNoteService.createNote(campaignId, request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Note created"));
    }

    @GetMapping
    @Operation(summary = "List GM session notes")
    public ResponseEntity<ApiResponse<List<GmSessionNoteResponse>>> listNotes(
            @PathVariable UUID campaignId, Authentication auth) {
        List<GmSessionNoteResponse> notes = gmSessionNoteService.listNotes(campaignId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(notes));
    }

    @GetMapping("/{noteId}")
    @Operation(summary = "Get GM session note")
    public ResponseEntity<ApiResponse<GmSessionNoteResponse>> getNote(
            @PathVariable UUID campaignId,
            @PathVariable UUID noteId, Authentication auth) {
        GmSessionNoteResponse response = gmSessionNoteService.getNote(noteId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{noteId}")
    @Operation(summary = "Update GM session note")
    public ResponseEntity<ApiResponse<GmSessionNoteResponse>> updateNote(
            @PathVariable UUID campaignId,
            @PathVariable UUID noteId,
            @Valid @RequestBody UpdateGmNoteRequest request, Authentication auth) {
        GmSessionNoteResponse response = gmSessionNoteService.updateNote(noteId, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Note updated"));
    }

    @DeleteMapping("/{noteId}")
    @Operation(summary = "Delete GM session note")
    public ResponseEntity<ApiResponse<Void>> deleteNote(
            @PathVariable UUID campaignId,
            @PathVariable UUID noteId, Authentication auth) {
        gmSessionNoteService.deleteNote(noteId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Note deleted"));
    }
}
