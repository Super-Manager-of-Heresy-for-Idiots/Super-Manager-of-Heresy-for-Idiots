package com.dnd.app.controller;

import com.dnd.app.dto.request.CreateArtifactRequest;
import com.dnd.app.dto.request.PlaceArtifactRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.ArtifactResponse;
import com.dnd.app.dto.response.InventorySlotResponse;
import com.dnd.app.service.ArtifactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/artifacts")
@RequiredArgsConstructor
public class ArtifactController {

    private final ArtifactService artifactService;

    @PostMapping
    public ResponseEntity<ApiResponse<ArtifactResponse>> create(
            @Valid @RequestBody CreateArtifactRequest request, Authentication auth) {
        ArtifactResponse artifact = artifactService.createArtifact(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(artifact, "Artifact created"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ArtifactResponse>>> list(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(artifactService.listArtifacts(auth.getName())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ArtifactResponse>> get(@PathVariable UUID id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(artifactService.getArtifact(id, auth.getName())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ArtifactResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody CreateArtifactRequest request, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(artifactService.updateArtifact(id, request, auth.getName()), "Artifact updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, Authentication auth) {
        artifactService.deleteArtifact(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Artifact deleted"));
    }

    @PutMapping("/place/{characterId}/{slot}")
    public ResponseEntity<ApiResponse<InventorySlotResponse>> placeArtifact(
            @PathVariable UUID characterId, @PathVariable String slot,
            @Valid @RequestBody PlaceArtifactRequest request, Authentication auth) {
        InventorySlotResponse resp = artifactService.placeArtifact(characterId, slot, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(resp, "Artifact placed in inventory"));
    }
}
