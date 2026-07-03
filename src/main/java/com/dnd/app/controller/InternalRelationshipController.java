package com.dnd.app.controller;

import com.dnd.app.dto.response.InternalRelationshipResponse;
import com.dnd.app.service.FriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service relationship projection consumed by the messenger service to authorize opening a
 * chat session. Secured by {@code X-Internal-Api-Key} (ROLE_INTERNAL_SERVICE), not a user JWT.
 */
@RestController
@RequestMapping("/api/internal/users/{userId}/relationships")
@RequiredArgsConstructor
@Tag(name = "Internal Relationships", description = "Service-to-service friendship/block checks")
public class InternalRelationshipController {

    private final FriendService friendService;

    @GetMapping("/{otherUserId}")
    @Operation(summary = "Whether two users are friends/blocked, with username snapshots")
    public ResponseEntity<InternalRelationshipResponse> getRelationship(
            @PathVariable UUID userId,
            @PathVariable UUID otherUserId) {
        return ResponseEntity.ok(friendService.resolveRelationship(userId, otherUserId));
    }
}
