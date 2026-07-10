package com.dnd.app.service;

import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.combat.HpChangeResult;
import com.dnd.app.dto.featurerule.RestResourcePreview;
import com.dnd.app.dto.response.ResourceResponse;
import com.dnd.app.dto.response.RestResult;
import com.dnd.app.dto.response.SpellSlotsResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Класс RestOrchestrationService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RestOrchestrationService {

    private static final String LONG_REST = "long_rest";
    private static final String SHORT_REST = "short_rest";

    private final PlayerCharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final CharacterResourceService characterResourceService;
    private final RestFeatureRuntimeService restFeatureRuntimeService;
    private final SpellSlotService spellSlotService;
    private final CharacterHpService characterHpService;
    private final CharacterHitDiceService characterHitDiceService;

    /**
     * Выполняет операции "rest" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param restTypeInput входящее значение rest type input, используемое бизнес-сценарием
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public RestResult rest(UUID campaignId, UUID characterId, String restTypeInput, String username) {
        String restCode = normalize(restTypeInput);
        boolean longRest = LONG_REST.equals(restCode);

        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));

        if (character.getCampaign() == null || !character.getCampaign().getId().equals(campaignId)) {
            throw new BadRequestException("Персонаж не принадлежит этой кампании");
        }
        boolean owner = character.getOwner() != null && character.getOwner().getId().equals(user.getId());
        boolean gm = campaignService.isGmInCampaign(campaignId, user.getId());
        if (!owner && !gm && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Нет прав на отдых этого персонажа");
        }

        // 1) Legacy custom resources (Rage/Ki/…): recovery per 081.
        List<ResourceResponse> resources = characterResourceService.restReset(characterId, restCode, username);
        // 2) Feature-rules resources + effects that end on this rest (no-op unless the subsystem is on).
        List<RestResourcePreview> featureResources = restFeatureRuntimeService.complete(character, restCode);
        // 3) Spell slots: a long rest restores all; a short rest has no generic recovery yet.
        SpellSlotsResponse spellSlots = longRest ? spellSlotService.restoreAll(characterId, username) : null;
        // 4) HP: a long rest restores to full and clears temp HP; a short rest waits on hit dice.
        HpChangeResult hp = longRest
                ? characterHpService.restoreToFull(characterId, campaignId, user.getId())
                : null;
        // 5) Hit dice: a long rest regains half (min 1); short-rest spending is a separate player action.
        if (longRest) {
            characterHitDiceService.restoreOnLongRest(character);
        }

        log.info("Rest orchestrated: characterId={}, type={}, by={}", characterId, restCode, username);
        return RestResult.builder()
                .restType(restCode)
                .resources(resources)
                .featureResources(featureResources)
                .spellSlots(spellSlots)
                .hp(hp)
                .build();
    }

    /** Accepts {@code long}/{@code short} (the API shorthand) as well as the canonical rest-type codes. */
    private String normalize(String input) {
        if (input == null) {
            return LONG_REST;
        }
        return switch (input.toLowerCase()) {
            case "long", LONG_REST -> LONG_REST;
            case "short", SHORT_REST -> SHORT_REST;
            default -> input.toLowerCase();
        };
    }
}
