package com.dnd.app.controller;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.DistributeXpRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/xp")
@RequiredArgsConstructor
@Tag(name = "XP Distribution", description = "Experience point distribution")
public class XpController {

    private final CampaignService campaignService;
    private final PlayerCharacterRepository playerCharacterRepository;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "Distribute XP to characters (GM only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> distributeXp(
            @PathVariable UUID campaignId,
            @Valid @RequestBody DistributeXpRequest request, Authentication auth) {

        User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Campaign campaign = campaignService.findCampaign(campaignId);

        if (user.getRole() != Role.ADMIN) {
            campaignService.enforceGmOrAdmin(campaign, user);
        }

        List<PlayerCharacter> targets;
        String target = request.getTarget().toUpperCase();

        switch (target) {
            case "ALL" -> targets = playerCharacterRepository.findByCampaignId(campaignId);
            case "SELECTED" -> {
                if (request.getCharacterIds() == null || request.getCharacterIds().isEmpty()) {
                    throw new BadRequestException("characterIds required for SELECTED target");
                }
                targets = playerCharacterRepository.findAllById(request.getCharacterIds());
                targets = targets.stream()
                        .filter(c -> c.getCampaign() != null && c.getCampaign().getId().equals(campaignId))
                        .toList();
            }
            case "SINGLE" -> {
                if (request.getCharacterIds() == null || request.getCharacterIds().size() != 1) {
                    throw new BadRequestException("Exactly one characterId required for SINGLE target");
                }
                PlayerCharacter pc = playerCharacterRepository.findById(request.getCharacterIds().get(0))
                        .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
                if (pc.getCampaign() == null || !pc.getCampaign().getId().equals(campaignId)) {
                    throw new BadRequestException("Character not in this campaign");
                }
                targets = List.of(pc);
            }
            default -> throw new BadRequestException("Invalid target: " + target + ". Use ALL, SELECTED, or SINGLE");
        }

        for (PlayerCharacter pc : targets) {
            pc.setExperience(pc.getExperience() + request.getAmount());
        }
        playerCharacterRepository.saveAll(targets);

        return ResponseEntity.ok(ApiResponse.ok(
                Map.of("charactersUpdated", targets.size(), "xpGranted", request.getAmount()),
                "XP distributed"
        ));
    }
}
