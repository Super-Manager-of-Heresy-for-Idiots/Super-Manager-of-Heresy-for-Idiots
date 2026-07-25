package com.dnd.app.service;

import com.dnd.app.domain.CampaignNpc;
import com.dnd.app.domain.NpcDialogue;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.DialogueActionType;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.PutNpcDialogueRequest;
import com.dnd.app.dto.response.DialogueNodeDto;
import com.dnd.app.dto.response.DialogueOptionDto;
import com.dnd.app.dto.response.NpcDialogueResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CampaignNpcRepository;
import com.dnd.app.repository.NpcDialogueRepository;
import com.dnd.app.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Класс NpcDialogueService описывает опциональный диалог NPC (WORLD_PLAN Этап 4). Мастер по
 * желанию сохраняет дерево реплик; NPC без диалога работает как прежде. Дерево хранится как
 * JSON-текст и валидируется при сохранении (уникальные id узлов, корректные ссылки/действия).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NpcDialogueService {

    private static final TypeReference<List<DialogueNodeDto>> NODE_LIST = new TypeReference<>() {
    };

    private final NpcDialogueRepository dialogueRepository;
    private final CampaignNpcRepository npcRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final ObjectMapper objectMapper;

    /**
     * Возвращает диалог NPC или null, если он не настроен. Игрок видит диалог только у видимого NPC.
     * @param campaignId идентификатор кампании
     * @param npcId идентификатор NPC
     * @param username имя пользователя
     * @return дерево диалога или null
     */
    @Transactional(readOnly = true)
    public NpcDialogueResponse getDialogue(UUID campaignId, UUID npcId, String username) {
        User user = getUser(username);
        CampaignNpc npc = findNpcInCampaign(npcId, campaignId);
        campaignService.enforceMembershipOrAdmin(npc.getCampaign(), user);
        if (!isGmOrAdmin(campaignId, user) && !Boolean.TRUE.equals(npc.getIsVisibleToPlayers())) {
            throw new ResourceNotFoundException("NPC not found");
        }
        return dialogueRepository.findByNpcId(npcId).map(this::toResponse).orElse(null);
    }

    /**
     * Сохраняет диалог NPC целиком (только мастер). Пустой список узлов удаляет диалог.
     * @param campaignId идентификатор кампании
     * @param npcId идентификатор NPC
     * @param request дерево диалога
     * @param username имя пользователя (мастер)
     * @return сохранённый диалог или null, если он был очищен
     */
    @Transactional
    public NpcDialogueResponse putDialogue(UUID campaignId, UUID npcId, PutNpcDialogueRequest request, String username) {
        User user = getUser(username);
        CampaignNpc npc = findNpcInCampaign(npcId, campaignId);
        campaignService.enforceGmOrAdmin(npc.getCampaign(), user);

        List<DialogueNodeDto> nodes = request.getNodes();
        if (nodes == null || nodes.isEmpty()) {
            dialogueRepository.findByNpcId(npcId).ifPresent(dialogueRepository::delete);
            log.info("NPC dialogue cleared: npcId={}, by={}", npcId, username);
            return null;
        }

        String rootNodeId = validateTree(nodes, request.getRootNodeId());

        NpcDialogue dialogue = dialogueRepository.findByNpcId(npcId)
                .orElseGet(() -> NpcDialogue.builder().npc(npc).build());
        dialogue.setRootNodeId(rootNodeId);
        dialogue.setTreeJson(serialize(nodes));
        dialogue = dialogueRepository.save(dialogue);

        log.info("NPC dialogue saved: npcId={}, nodes={}, by={}", npcId, nodes.size(), username);
        return toResponse(dialogue);
    }

    /**
     * Удаляет диалог NPC, если он есть (только мастер).
     * @param campaignId идентификатор кампании
     * @param npcId идентификатор NPC
     * @param username имя пользователя (мастер)
     */
    @Transactional
    public void deleteDialogue(UUID campaignId, UUID npcId, String username) {
        User user = getUser(username);
        CampaignNpc npc = findNpcInCampaign(npcId, campaignId);
        campaignService.enforceGmOrAdmin(npc.getCampaign(), user);
        dialogueRepository.findByNpcId(npcId).ifPresent(dialogueRepository::delete);
        log.info("NPC dialogue deleted: npcId={}, by={}", npcId, username);
    }

    /**
     * Диалог NPC для встраивания в агрегированное взаимодействие; null, если не настроен.
     * Авторизацию/видимость проверяет вызывающий {@code NpcInteractionService}.
     * @param npcId идентификатор NPC
     * @return дерево диалога или null
     */
    @Transactional(readOnly = true)
    public NpcDialogueResponse resolveForInteraction(UUID npcId) {
        return dialogueRepository.findByNpcId(npcId).map(this::toResponse).orElse(null);
    }

    // --- Private helpers ---

    /** Проверяет дерево и возвращает корректный rootNodeId (первый узел, если не задан). */
    private String validateTree(List<DialogueNodeDto> nodes, String requestedRoot) {
        Set<String> ids = new HashSet<>();
        for (DialogueNodeDto node : nodes) {
            if (node.getId() == null || node.getId().isBlank()) {
                throw new BadRequestException("Each dialogue node requires a non-blank id");
            }
            if (!ids.add(node.getId())) {
                throw new BadRequestException("Duplicate dialogue node id: " + node.getId());
            }
        }
        for (DialogueNodeDto node : nodes) {
            List<DialogueOptionDto> options = node.getOptions();
            if (options == null) {
                continue;
            }
            for (DialogueOptionDto option : options) {
                boolean hasNext = option.getNextNodeId() != null && !option.getNextNodeId().isBlank();
                boolean hasAction = option.getActionType() != null && !option.getActionType().isBlank();
                if (!hasNext && !hasAction) {
                    throw new BadRequestException("Dialogue option must have a nextNodeId or an action");
                }
                if (hasNext && !ids.contains(option.getNextNodeId())) {
                    throw new BadRequestException("Dialogue option points to unknown node: " + option.getNextNodeId());
                }
                if (hasAction) {
                    try {
                        DialogueActionType.valueOf(option.getActionType().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new BadRequestException("Invalid dialogue action: " + option.getActionType(), e);
                    }
                }
            }
        }
        if (requestedRoot != null && !requestedRoot.isBlank()) {
            if (!ids.contains(requestedRoot)) {
                throw new BadRequestException("rootNodeId points to unknown node: " + requestedRoot);
            }
            return requestedRoot;
        }
        return nodes.get(0).getId();
    }

    private String serialize(List<DialogueNodeDto> nodes) {
        try {
            return objectMapper.writeValueAsString(nodes);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Could not serialize dialogue tree", e);
        }
    }

    private List<DialogueNodeDto> deserialize(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, NODE_LIST);
        } catch (JsonProcessingException e) {
            log.warn("Corrupt dialogue tree JSON, returning empty: {}", e.getMessage());
            return List.of();
        }
    }

    private NpcDialogueResponse toResponse(NpcDialogue dialogue) {
        return NpcDialogueResponse.builder()
                .rootNodeId(dialogue.getRootNodeId())
                .nodes(deserialize(dialogue.getTreeJson()))
                .build();
    }

    private CampaignNpc findNpcInCampaign(UUID npcId, UUID campaignId) {
        CampaignNpc npc = npcRepository.findById(npcId)
                .orElseThrow(() -> new ResourceNotFoundException("NPC not found"));
        if (npc.getCampaign() == null || !npc.getCampaign().getId().equals(campaignId)) {
            throw new ResourceNotFoundException("NPC not found in this campaign");
        }
        return npc;
    }

    private boolean isGmOrAdmin(UUID campaignId, User user) {
        return user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId());
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
