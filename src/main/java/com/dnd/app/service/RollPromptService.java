package com.dnd.app.service;

import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.RollPrompt;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.RollPromptStatus;
import com.dnd.app.domain.enums.RollPromptType;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.request.CreateRollPromptRequest;
import com.dnd.app.dto.response.RollPromptResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.RollPromptRepository;
import com.dnd.app.repository.StatTypeRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.service.combat.DiceRoller;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Класс RollPromptService описывает флоу "мастер запрашивает проверку" (ROLL_PROMPT):
 * мастер создаёт запрос для персонажей, у владельцев появляется окно броска
 * ({@code ROLL_PROMPT_CREATED}), игрок жмёт "Бросить" — d20 исполняется на сервере
 * (честность: клиент не присылает результат), итог сохраняется и рассылается
 * ({@code ROLL_PROMPT_RESOLVED}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RollPromptService {

    private final RollPromptRepository rollPromptRepository;
    private final PlayerCharacterRepository characterRepository;
    private final StatTypeRepository statTypeRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final CharacterEffectService characterEffectService;
    private final WebSocketEventService webSocketEventService;
    private final DiceRoller diceRoller;
    private final ObjectMapper objectMapper;

    /**
     * Мастер запрашивает проверку у одного или нескольких персонажей.
     * @param campaignId идентификатор кампании
     * @param request параметры проверки (тип, характеристика, DC, режим)
     * @param username имя пользователя-мастера
     * @return созданные запросы
     */
    @Transactional
    public List<RollPromptResponse> createPrompts(UUID campaignId, CreateRollPromptRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);

        StatType statType = null;
        if (request.getRollType() != RollPromptType.CUSTOM) {
            if (request.getStatTypeId() == null) {
                throw new BadRequestException("statTypeId is required for this roll type");
            }
            statType = statTypeRepository.findById(request.getStatTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stat type not found"));
        }

        List<RollPromptResponse> created = new ArrayList<>();
        for (UUID characterId : request.getCharacterIds()) {
            PlayerCharacter character = characterRepository.findById(characterId)
                    .orElseThrow(() -> new ResourceNotFoundException("Character not found: " + characterId));
            if (character.getCampaign() == null || !character.getCampaign().getId().equals(campaignId)) {
                throw new BadRequestException("Character does not belong to this campaign: " + characterId);
            }

            RollPrompt prompt = RollPrompt.builder()
                    .campaign(campaign)
                    .character(character)
                    .requestedBy(user)
                    .rollType(request.getRollType())
                    .statType(statType)
                    .dc(request.getDc())
                    .hideDc(Boolean.TRUE.equals(request.getHideDc()))
                    .advantageMode(request.getAdvantageMode() != null ? request.getAdvantageMode() : "NORMAL")
                    .description(request.getDescription())
                    .status(RollPromptStatus.PENDING)
                    .build();
            prompt = rollPromptRepository.save(prompt);

            webSocketEventService.sendCampaignEvent(WebSocketEventType.ROLL_PROMPT_CREATED,
                    campaignId, characterId,
                    Map.of("promptId", prompt.getId(), "characterId", characterId), user.getId());
            created.add(toResponse(prompt, true));
        }

        log.info("Roll prompts created: campaignId={}, count={}, type={}, by={}",
                campaignId, created.size(), request.getRollType(), username);
        return created;
    }

    /**
     * Возвращает запросы проверок: мастер видит все (с фильтром по статусу), игрок —
     * только запросы своих персонажей (DC скрыт, пока hideDc и бросок не совершён).
     * @param campaignId идентификатор кампании
     * @param status фильтр по статусу (опционально)
     * @param username имя пользователя, выполняющего запрос
     * @return список запросов
     */
    @Transactional(readOnly = true)
    public List<RollPromptResponse> listPrompts(UUID campaignId, RollPromptStatus status, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        boolean gm = isGmOrAdmin(campaignId, user);

        List<RollPrompt> prompts;
        if (gm) {
            prompts = status != null
                    ? rollPromptRepository.findByCampaignIdAndStatusOrderByCreatedAtDesc(campaignId, status)
                    : rollPromptRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId);
        } else {
            prompts = status != null
                    ? rollPromptRepository.findByCampaignIdAndCharacter_Owner_IdAndStatusOrderByCreatedAtAsc(
                            campaignId, user.getId(), status)
                    : rollPromptRepository.findByCampaignIdAndCharacter_Owner_IdOrderByCreatedAtDesc(
                            campaignId, user.getId());
        }
        return prompts.stream().map(p -> toResponse(p, gm)).toList();
    }

    /**
     * Игрок совершает бросок по запросу. d20 бросается на сервере; модификатор
     * рассчитывается по персонажу (характеристика, баффы, при спасброске — владение).
     * @param campaignId идентификатор кампании
     * @param promptId идентификатор запроса
     * @param username имя пользователя, выполняющего бросок (владелец персонажа или ГМ)
     * @return запрос с заполненным результатом
     */
    @Transactional
    public RollPromptResponse roll(UUID campaignId, UUID promptId, String username) {
        User user = getUser(username);
        RollPrompt prompt = findPrompt(promptId, campaignId);
        campaignService.enforceMembershipOrAdmin(prompt.getCampaign(), user);
        boolean gm = isGmOrAdmin(campaignId, user);

        PlayerCharacter character = prompt.getCharacter();
        if (!gm && (character.getOwner() == null || !character.getOwner().getId().equals(user.getId()))) {
            throw new AccessDeniedException("Only the character's owner can make this roll");
        }
        if (prompt.getStatus() != RollPromptStatus.PENDING) {
            throw new BadRequestException("This roll prompt has already been resolved");
        }

        int modifier = computeModifier(prompt, username);

        int first = diceRoller.rollD20();
        Integer second = null;
        int kept = first;
        if ("ADVANTAGE".equals(prompt.getAdvantageMode()) || "DISADVANTAGE".equals(prompt.getAdvantageMode())) {
            second = diceRoller.rollD20();
            kept = "ADVANTAGE".equals(prompt.getAdvantageMode())
                    ? Math.max(first, second)
                    : Math.min(first, second);
        }

        int total = kept + modifier;
        prompt.setRollNatural(kept);
        prompt.setRollSecond(second != null ? (second == kept ? first : second) : null);
        prompt.setModifier(modifier);
        prompt.setTotal(total);
        prompt.setSuccess(prompt.getDc() != null ? total >= prompt.getDc() : null);
        prompt.setStatus(RollPromptStatus.ROLLED);
        prompt.setRolledAt(Instant.now());
        prompt = rollPromptRepository.save(prompt);

        log.info("Roll prompt resolved: promptId={}, characterId={}, d20={}, mod={}, total={}, success={}, by={}",
                promptId, character.getId(), kept, modifier, total, prompt.getSuccess(), username);

        Map<String, Object> data = new HashMap<>();
        data.put("promptId", prompt.getId());
        data.put("characterId", character.getId());
        data.put("characterName", character.getName());
        data.put("rollNatural", kept);
        data.put("modifier", modifier);
        data.put("total", total);
        if (prompt.getSuccess() != null) {
            data.put("success", prompt.getSuccess());
        }
        webSocketEventService.sendCampaignEvent(WebSocketEventType.ROLL_PROMPT_RESOLVED,
                campaignId, character.getId(), data, user.getId());

        return toResponse(prompt, true);
    }

    /**
     * Мастер отменяет ожидающий запрос проверки.
     * @param campaignId идентификатор кампании
     * @param promptId идентификатор запроса
     * @param username имя пользователя-мастера
     */
    @Transactional
    public void cancel(UUID campaignId, UUID promptId, String username) {
        User user = getUser(username);
        RollPrompt prompt = findPrompt(promptId, campaignId);
        campaignService.enforceGmOrAdmin(prompt.getCampaign(), user);
        if (prompt.getStatus() != RollPromptStatus.PENDING) {
            throw new BadRequestException("Only a pending prompt can be cancelled");
        }

        prompt.setStatus(RollPromptStatus.CANCELLED);
        rollPromptRepository.save(prompt);

        log.info("Roll prompt cancelled: promptId={}, by={}", promptId, username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.ROLL_PROMPT_CANCELLED,
                campaignId, prompt.getCharacter().getId(),
                Map.of("promptId", promptId, "characterId", prompt.getCharacter().getId()), user.getId());
    }

    // --- Private helpers ---

    /**
     * Модификатор броска: CUSTOM — 0; проверка характеристики — итог из
     * {@link CharacterEffectService#calculateAbilityCheckModifier}; спасбросок —
     * то же + бонус мастерства при владении этим спасброском.
     */
    private int computeModifier(RollPrompt prompt, String username) {
        if (prompt.getRollType() == RollPromptType.CUSTOM || prompt.getStatType() == null) {
            return 0;
        }
        UUID characterId = prompt.getCharacter().getId();
        UUID statTypeId = prompt.getStatType().getId();
        int base = characterEffectService
                .calculateAbilityCheckModifier(characterId, statTypeId, username)
                .getTotalModifier();

        if (prompt.getRollType() == RollPromptType.SAVING_THROW
                && isSaveProficient(prompt.getCharacter(), statTypeId)) {
            base += proficiencyBonus(prompt.getCharacter().getTotalLevel());
        }
        return base;
    }

    private boolean isSaveProficient(PlayerCharacter character, UUID statTypeId) {
        String json = character.getSavingThrowProficiencyStatIdsJson();
        if (json == null || json.isBlank()) {
            return false;
        }
        try {
            List<String> ids = objectMapper.readValue(json, new TypeReference<List<String>>() { });
            return ids.stream().anyMatch(id -> id.equalsIgnoreCase(statTypeId.toString()));
        } catch (Exception e) {
            log.warn("Corrupted savingThrowProficiencyStatIdsJson for character {}: {}",
                    character.getId(), e.getMessage());
            return false;
        }
    }

    private static int proficiencyBonus(Integer totalLevel) {
        int level = totalLevel != null ? totalLevel : 1;
        return 2 + Math.max(0, level - 1) / 4;
    }

    private RollPrompt findPrompt(UUID promptId, UUID campaignId) {
        RollPrompt prompt = rollPromptRepository.findById(promptId)
                .orElseThrow(() -> new ResourceNotFoundException("Roll prompt not found"));
        if (prompt.getCampaign() == null || !prompt.getCampaign().getId().equals(campaignId)) {
            throw new ResourceNotFoundException("Roll prompt not found in this campaign");
        }
        return prompt;
    }

    private boolean isGmOrAdmin(UUID campaignId, User user) {
        return user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId());
    }

    private RollPromptResponse toResponse(RollPrompt prompt, boolean gmView) {
        boolean dcVisible = gmView
                || !Boolean.TRUE.equals(prompt.getHideDc())
                || prompt.getStatus() == RollPromptStatus.ROLLED;
        return RollPromptResponse.builder()
                .id(prompt.getId())
                .campaignId(prompt.getCampaign().getId())
                .characterId(prompt.getCharacter().getId())
                .characterName(prompt.getCharacter().getName())
                .ownerUserId(prompt.getCharacter().getOwner() != null
                        ? prompt.getCharacter().getOwner().getId() : null)
                .rollType(prompt.getRollType())
                .statTypeId(prompt.getStatType() != null ? prompt.getStatType().getId() : null)
                .statName(prompt.getStatType() != null ? prompt.getStatType().getNameRu() : null)
                .dc(dcVisible ? prompt.getDc() : null)
                .hideDc(prompt.getHideDc())
                .advantageMode(prompt.getAdvantageMode())
                .description(prompt.getDescription())
                .status(prompt.getStatus())
                .requestedByName(prompt.getRequestedBy() != null ? prompt.getRequestedBy().getUsername() : null)
                .rollNatural(prompt.getRollNatural())
                .rollSecond(prompt.getRollSecond())
                .modifier(prompt.getModifier())
                .total(prompt.getTotal())
                .success(prompt.getSuccess())
                .createdAt(prompt.getCreatedAt())
                .rolledAt(prompt.getRolledAt())
                .build();
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
