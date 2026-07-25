package com.dnd.app.service;

import com.dnd.app.domain.CampActivity;
import com.dnd.app.domain.CampParticipant;
import com.dnd.app.domain.CampSession;
import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.request.AssignCampActivityRequest;
import com.dnd.app.dto.request.CreateCampActivityRequest;
import com.dnd.app.dto.response.CampActivityResponse;
import com.dnd.app.dto.response.CampSessionResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.mapper.CampMapper;
import com.dnd.app.repository.CampActivityRepository;
import com.dnd.app.repository.CampParticipantRepository;
import com.dnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Класс CampActivityService описывает справочник даунтайм-активностей привала и их
 * назначение участникам. Механику активности не применяют: система фиксирует, чем занят
 * персонаж, а награды мастер выдаёт обычными инструментами (предметы, опыт, эффекты).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampActivityService {

    private final CampActivityRepository campActivityRepository;
    private final CampParticipantRepository campParticipantRepository;
    private final UserRepository userRepository;
    private final CampService campService;
    private final CampaignService campaignService;
    private final WebSocketEventService webSocketEventService;
    private final CampMapper campMapper;

    /**
     * Возвращает активности, доступные кампании: системные плюс её собственные.
     * @param campaignId идентификатор кампании
     * @param username имя пользователя, выполняющего запрос
     * @return справочник активностей
     */
    @Transactional(readOnly = true)
    public List<CampActivityResponse> listActivities(UUID campaignId, String username) {
        User user = getUser(username);
        campaignService.enforceMembershipOrAdmin(campaignService.findCampaign(campaignId), user);
        return campActivityRepository.findAvailableForCampaign(campaignId).stream()
                .map(campMapper::toActivityResponse)
                .toList();
    }

    /**
     * Создаёт кастомную активность кампании.
     * @param campaignId идентификатор кампании
     * @param request параметры активности
     * @param username имя пользователя-мастера
     * @return созданная активность
     */
    @Transactional
    public CampActivityResponse createActivity(UUID campaignId, CreateCampActivityRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);

        CampActivity activity = campActivityRepository.save(CampActivity.builder()
                .campaign(campaign)
                .name(request.getName())
                .description(request.getDescription())
                .iconCode(request.getIconCode())
                .skillCheckRef(request.getSkillCheckRef())
                .createdBy(user)
                .build());

        log.info("Camp activity created: campaignId={}, activityId={}, by={}",
                campaignId, activity.getId(), username);
        return campMapper.toActivityResponse(activity);
    }

    /**
     * Удаляет кастомную активность кампании. Системные активности не трогаются,
     * назначения у участников снимаются вместе с активностью.
     * @param campaignId идентификатор кампании
     * @param activityId идентификатор активности
     * @param username имя пользователя-мастера
     */
    @Transactional
    public void deleteActivity(UUID campaignId, UUID activityId, String username) {
        User user = getUser(username);
        campaignService.enforceGmOrAdmin(campaignService.findCampaign(campaignId), user);

        CampActivity activity = campActivityRepository.findByIdAndCampaignId(activityId, campaignId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Кастомная активность кампании не найдена"));

        for (CampParticipant participant : campParticipantRepository.findByActivityId(activityId)) {
            participant.setActivity(null);
            campParticipantRepository.save(participant);
        }
        campActivityRepository.delete(activity);
        log.info("Camp activity deleted: campaignId={}, activityId={}, by={}", campaignId, activityId, username);
    }

    /**
     * Назначает участнику привала даунтайм-активность или снимает её (пустой activityId).
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param characterId идентификатор персонажа
     * @param request активность и заметка мастера
     * @param username имя пользователя-мастера
     * @return состояние привала после назначения
     */
    @Transactional
    public CampSessionResponse assignActivity(UUID campaignId, UUID campId, UUID characterId,
                                              AssignCampActivityRequest request, String username) {
        User user = campService.requireGmForChange(campaignId, username);
        CampSession camp = campService.findCampForUpdate(campaignId, campId);
        campService.enforceNotCompleted(camp);

        CampParticipant participant = campParticipantRepository
                .findByCampSessionIdAndCharacterId(campId, characterId)
                .orElseThrow(() -> new BadRequestException("Персонаж не участвует в привале"));

        CampActivity activity = null;
        if (request != null && request.getActivityId() != null) {
            activity = campActivityRepository
                    .findAvailableForCampaign(request.getActivityId(), campaignId)
                    .orElseThrow(() -> new ResourceNotFoundException("Активность недоступна этой кампании"));
        }

        participant.setActivity(activity);
        participant.setActivityNote(request != null ? request.getNote() : null);
        campParticipantRepository.save(participant);

        log.info("Camp activity assigned: campId={}, characterId={}, activityId={}, by={}",
                campId, characterId, activity != null ? activity.getId() : null, username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.CAMP_PARTICIPANT_UPDATED, campaignId,
                characterId, Map.of("campId", campId, "characterId", characterId), user.getId());

        return campService.toResponse(camp, user);
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }
}
