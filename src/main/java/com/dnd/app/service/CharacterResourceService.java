package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.featurerule.FormulaRoundingMode;
import com.dnd.app.dto.request.ModifyResourceRequest;
import com.dnd.app.dto.response.ResourceResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import com.dnd.app.service.formula.CharacterFormulaContextFactory;
import com.dnd.app.service.formula.FormulaContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterResourceService {

    private final CharacterResourceRepository characterResourceRepository;
    private final CustomResourceTypeRepository customResourceTypeRepository;
    private final PlayerCharacterRepository playerCharacterRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final CharacterFormulaContextFactory formulaContextFactory;
    private final FeatureFormulaService featureFormulaService;

    @Transactional
    public List<ResourceResponse> getResources(UUID characterId, String username) {
        User user = getUser(username);
        PlayerCharacter character = findCharacter(characterId);
        enforceViewAccess(character, user);

        FormulaContext ctx = formulaContextFactory.build(character);
        provisionClassResources(character, ctx);

        return characterResourceRepository.findByCharacterId(characterId).stream()
                .map(r -> toResponse(r, ctx))
                .toList();
    }

    /**
     * Ensures the character has the resources bound to its class(es) (Rage for Barbarian, Ki for Monk, …).
     * Idempotent self-heal: creates only the missing rows, starting full. Resource→class binding lives on
     * {@code custom_resource_types.class_bound_id} (populated by the content import). Non-class resources
     * (e.g. Luck Points from the Lucky feat) are not provisioned here.
     */
    private void provisionClassResources(PlayerCharacter character, FormulaContext ctx) {
        List<UUID> classIds = character.getClassLevels().stream()
                .map(CharacterClassLevel::getClassId)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (classIds.isEmpty()) {
            return;
        }
        for (CustomResourceType type : customResourceTypeRepository.findByClassBound_IdIn(classIds)) {
            boolean present = characterResourceRepository
                    .findByCharacterIdAndResourceTypeId(character.getId(), type.getId()).isPresent();
            if (!present) {
                Integer max = effectiveMax(type, ctx);
                characterResourceRepository.save(CharacterResource.builder()
                        .character(character)
                        .resourceType(type)
                        .currentValue(max != null ? max : 0)
                        .build());
            }
        }
    }

    /** Effective max: the character-evaluated {@code max_formula} when present, else the fixed max_value. */
    private Integer effectiveMax(CustomResourceType type, FormulaContext ctx) {
        String expr = type.getMaxFormula();
        if (expr != null && !expr.isBlank() && ctx != null) {
            try {
                return featureFormulaService.evaluateInteger(expr, ctx, FormulaRoundingMode.FLOOR, 0.0, null);
            } catch (RuntimeException e) {
                log.warn("Resource max formula failed for type {}: {}", type.getId(), e.getMessage());
            }
        }
        return type.getMaxValue();
    }

    @Transactional
    public ResourceResponse modifyResource(UUID characterId, ModifyResourceRequest request, String username) {
        User user = getUser(username);
        PlayerCharacter character = findCharacter(characterId);
        enforceOwnerOrGmOrAdmin(character, user);

        CharacterResource resource = characterResourceRepository
                .findByCharacterIdAndResourceTypeId(characterId, request.getResourceTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Character resource not found"));

        FormulaContext ctx = formulaContextFactory.build(character);
        Integer max = effectiveMax(resource.getResourceType(), ctx);

        int newValue = request.getCurrentValue();
        if (newValue < 0) {
            newValue = 0;
        }
        if (max != null && newValue > max) {
            newValue = max;
        }

        resource.setCurrentValue(newValue);
        resource = characterResourceRepository.save(resource);

        log.info("Resource modified: characterId={}, resourceTypeId={}, newValue={}, by={}",
                characterId, request.getResourceTypeId(), newValue, username);
        return toResponse(resource, ctx);
    }

    @Transactional
    public ResourceResponse addResource(UUID characterId, UUID resourceTypeId, String username) {
        User user = getUser(username);
        PlayerCharacter character = findCharacter(characterId);
        enforceOwnerOrGmOrAdmin(character, user);

        CustomResourceType resourceType = customResourceTypeRepository.findById(resourceTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource type not found"));

        // Check if already exists
        characterResourceRepository.findByCharacterIdAndResourceTypeId(characterId, resourceTypeId)
                .ifPresent(existing -> {
                    throw new BadRequestException("Character already has this resource type");
                });

        FormulaContext ctx = formulaContextFactory.build(character);
        Integer max = effectiveMax(resourceType, ctx);
        CharacterResource resource = CharacterResource.builder()
                .character(character)
                .resourceType(resourceType)
                .currentValue(max != null ? max : 0)
                .build();
        resource = characterResourceRepository.save(resource);

        log.info("Resource added: characterId={}, resourceTypeId={}, by={}", characterId, resourceTypeId, username);
        return toResponse(resource, ctx);
    }

    /**
     * Refills resources that reset on the given rest back to their (formula-evaluated) max. A short rest refills
     * only {@code short_rest} resources; a long rest refills both {@code short_rest} and {@code long_rest} ones.
     */
    @Transactional
    public List<ResourceResponse> restReset(UUID characterId, String restType, String username) {
        User user = getUser(username);
        PlayerCharacter character = findCharacter(characterId);
        enforceOwnerOrGmOrAdmin(character, user);

        boolean longRest = "long_rest".equalsIgnoreCase(restType);
        FormulaContext ctx = formulaContextFactory.build(character);

        for (CharacterResource r : characterResourceRepository.findByCharacterId(characterId)) {
            String resetOn = r.getResourceType().getResetOn();
            boolean refill = "short_rest".equals(resetOn) || (longRest && "long_rest".equals(resetOn));
            if (refill) {
                Integer max = effectiveMax(r.getResourceType(), ctx);
                r.setCurrentValue(max != null ? max : 0);
                characterResourceRepository.save(r);
            }
        }
        log.info("Rest reset: characterId={}, restType={}, by={}", characterId, longRest ? "long_rest" : "short_rest", username);
        return characterResourceRepository.findByCharacterId(characterId).stream()
                .map(r -> toResponse(r, ctx))
                .toList();
    }

    // --- Private helpers ---

    private void enforceViewAccess(PlayerCharacter character, User user) {
        if (user.getRole() == Role.ADMIN) return;
        if (character.getCampaign() != null) {
            if (!campaignService.isMemberOfCampaign(character.getCampaign().getId(), user.getId())) {
                throw new AccessDeniedException("You are not a member of this character's campaign");
            }
        } else {
            if (!character.getOwner().getId().equals(user.getId())) {
                throw new AccessDeniedException("You cannot view this character's resources");
            }
        }
    }

    private void enforceOwnerOrGmOrAdmin(PlayerCharacter character, User user) {
        if (user.getRole() == Role.ADMIN) return;
        if (character.getOwner().getId().equals(user.getId())) return;
        if (character.getCampaign() != null
                && campaignService.isGmInCampaign(character.getCampaign().getId(), user.getId())) return;
        throw new AccessDeniedException("Only the character owner, campaign GM, or ADMIN can modify resources");
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private PlayerCharacter findCharacter(UUID characterId) {
        return playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
    }

    private ResourceResponse toResponse(CharacterResource resource, FormulaContext ctx) {
        return ResourceResponse.builder()
                .resourceTypeId(resource.getResourceType().getId())
                .resourceName(resource.getResourceType().getName())
                .currentValue(resource.getCurrentValue())
                .maxValue(effectiveMax(resource.getResourceType(), ctx))
                .build();
    }
}
