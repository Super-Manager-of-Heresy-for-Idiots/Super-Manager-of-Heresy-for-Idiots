package com.dnd.app.service;

import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.request.DistributeXpRequest;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class XpService {

    private final CampaignService campaignService;
    private final PlayerCharacterRepository playerCharacterRepository;
    private final UserRepository userRepository;
    private final WebSocketEventService webSocketEventService;

    @Transactional
    public Map<String, Object> distributeXp(UUID campaignId, DistributeXpRequest request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);

        if (request.getAmount() == null) {
            throw new BadRequestException("amount is required");
        }

        List<PlayerCharacter> targets;
        String target = request.getTarget() == null ? "" : request.getTarget().toUpperCase();
        switch (target) {
            case "ALL" -> targets = playerCharacterRepository.findByCampaignId(campaignId);
            case "SELECTED" -> {
                if (request.getCharacterIds() == null || request.getCharacterIds().isEmpty()) {
                    throw new BadRequestException("characterIds required for SELECTED target");
                }
                targets = playerCharacterRepository.findAllById(request.getCharacterIds()).stream()
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

        log.info("XP distributed: campaignId={}, target={}, count={}, amount={}, by={}",
                campaignId, target, targets.size(), request.getAmount(), username);

        List<UUID> affectedCharacterIds = targets.stream().map(PlayerCharacter::getId).toList();
        webSocketEventService.sendCampaignEvent(WebSocketEventType.XP_GRANTED, campaignId,
                Map.of("amount", request.getAmount(), "characterIds", affectedCharacterIds),
                user.getId());

        return Map.of(
                "charactersUpdated", targets.size(),
                "xpGranted", request.getAmount()
        );
    }
}
