package com.dnd.app.service;

import com.dnd.app.domain.CampParticipant;
import com.dnd.app.domain.CampSession;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.CampParticipantState;
import com.dnd.app.domain.enums.CampStatus;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.request.ApplyPartialRestRequest;
import com.dnd.app.dto.request.CompleteCampRestRequest;
import com.dnd.app.dto.request.StartCampRestRequest;
import com.dnd.app.dto.response.CampRestFailureResponse;
import com.dnd.app.dto.response.CampRestSummaryResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CampParticipantRepository;
import com.dnd.app.repository.CampSessionRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Класс CampRestService описывает групповой отдых привала поверх {@link RestOrchestrationService}.
 * Отдых применяется per-character: у каждого участника собственная транзакция, поэтому сбой
 * одного не откатывает остальных — ошибки возвращаются отдельным списком с возможностью повтора.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampRestService {

    private static final String LONG_REST = "long_rest";
    private static final String SHORT_REST = "short_rest";

    private final CampSessionRepository campSessionRepository;
    private final CampParticipantRepository campParticipantRepository;
    private final CampParticipantRestExecutor restExecutor;
    private final CampService campService;
    private final WebSocketEventService webSocketEventService;
    private final EntityManager entityManager;

    /**
     * Объявляет привал: переводит лагерь в RESTING и помечает участников как отдыхающих.
     * Листы персонажей на этом шаге не меняются — восстановление применяется отдельным вызовом.
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param request тип отдыха
     * @param username имя пользователя-мастера
     * @return обновлённое состояние привала
     */
    @Transactional
    public CampRestSummaryResponse startRest(UUID campaignId, UUID campId,
                                             StartCampRestRequest request, String username) {
        User user = campService.requireGmForChange(campaignId, username);
        CampSession camp = campService.findCampForUpdate(campaignId, campId);
        if (camp.getStatus() != CampStatus.ACTIVE) {
            throw new BadRequestException("Отдых доступен только в стоящем лагере (ACTIVE)");
        }

        String restType = normalizeRestType(request.getRestType());
        List<CampParticipant> participants = campParticipantRepository
                .findByCampSessionIdOrderByCreatedAtAsc(campId);
        if (participants.isEmpty()) {
            throw new BadRequestException("В привале нет участников");
        }

        camp.setStatus(CampStatus.RESTING);
        camp.setRestType(restType);
        camp.setRestStartedAt(Instant.now());
        camp.setRestCompletedAt(null);
        campSessionRepository.save(camp);

        for (CampParticipant participant : participants) {
            participant.setState(CampParticipantState.RESTING);
            participant.setRestErrorCode(null);
            participant.setRestErrorMessage(null);
            campParticipantRepository.save(participant);
        }

        log.info("Camp rest started: campId={}, type={}, participants={}, by={}",
                campId, restType, participants.size(), username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.CAMP_REST_STARTED, campaignId,
                Map.of("campId", campId, "restType", restType), user.getId());

        return CampRestSummaryResponse.builder()
                .campId(campId)
                .restType(restType)
                .partial(false)
                .restedCount(0)
                .failedCount(0)
                .skippedCount(0)
                .failures(List.of())
                .camp(campService.toResponse(camp, user))
                .build();
    }

    /**
     * Применяет объявленный отдых участникам привала. Пустой список персонажей — применить
     * всем, у кого отдых ещё не применён; непустой используется для повтора после ошибки.
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param request точечный список персонажей (опционально)
     * @param username имя пользователя-мастера
     * @return сводка отдыха: успешные, пропущенные и ошибки
     */
    @Transactional
    public CampRestSummaryResponse completeRest(UUID campaignId, UUID campId,
                                                CompleteCampRestRequest request, String username) {
        User user = campService.requireGmForChange(campaignId, username);
        CampSession camp = campService.findCampForUpdate(campaignId, campId);
        if (camp.getStatus() != CampStatus.RESTING) {
            throw new BadRequestException("Отдых не объявлен — нечего применять");
        }

        String restType = camp.getRestType() != null ? camp.getRestType() : LONG_REST;
        List<CampParticipant> targets = selectTargets(campId,
                request != null ? request.getCharacterIds() : null,
                Set.of(CampParticipantState.RESTING, CampParticipantState.NOT_RESTED,
                        CampParticipantState.FAILED));

        CampRestSummaryResponse summary = applyRest(campaignId, camp, targets, restType,
                CampParticipantState.RESTED, username);

        boolean pending = campParticipantRepository.findByCampSessionIdOrderByCreatedAtAsc(campId).stream()
                .anyMatch(participant -> participant.getState() == CampParticipantState.RESTING);
        if (!pending) {
            camp.setRestCompletedAt(Instant.now());
            campSessionRepository.save(camp);
        }

        log.info("Camp rest applied: campId={}, type={}, rested={}, failed={}, by={}",
                campId, restType, summary.getRestedCount(), summary.getFailedCount(), username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.CAMP_REST_COMPLETED, campaignId,
                Map.of("campId", campId, "restType", restType,
                        "restedCount", summary.getRestedCount(),
                        "failedCount", summary.getFailedCount()), user.getId());

        summary.setCamp(campService.toResponse(camp, user));
        return summary;
    }

    /**
     * Засчитывает частичный отдых после прерывания. По умолчанию прерванный привал
     * восстановления не даёт, поэтому применение — явное решение мастера; механически
     * частичный отдых применяется как короткий привал, отдельных правил не вводится.
     * @param campaignId идентификатор кампании
     * @param campId идентификатор привала
     * @param request точечный список персонажей (опционально)
     * @param username имя пользователя-мастера
     * @return сводка частичного отдыха
     */
    @Transactional
    public CampRestSummaryResponse applyPartialRest(UUID campaignId, UUID campId,
                                                    ApplyPartialRestRequest request, String username) {
        User user = campService.requireGmForChange(campaignId, username);
        CampSession camp = campService.findCampForUpdate(campaignId, campId);
        if (camp.getStatus() != CampStatus.INTERRUPTED) {
            throw new BadRequestException("Частичный отдых засчитывается только прерванному привалу");
        }

        List<CampParticipant> targets = selectTargets(campId,
                request != null ? request.getCharacterIds() : null,
                Set.of(CampParticipantState.NOT_RESTED, CampParticipantState.RESTING,
                        CampParticipantState.FAILED));

        CampRestSummaryResponse summary = applyRest(campaignId, camp, targets, SHORT_REST,
                CampParticipantState.PARTIAL, username);

        camp.setApplyPartialRest(true);
        campSessionRepository.save(camp);

        log.info("Camp partial rest applied: campId={}, rested={}, failed={}, by={}",
                campId, summary.getRestedCount(), summary.getFailedCount(), username);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.CAMP_REST_COMPLETED, campaignId,
                Map.of("campId", campId, "restType", SHORT_REST, "partial", true,
                        "restedCount", summary.getRestedCount(),
                        "failedCount", summary.getFailedCount()), user.getId());

        summary.setCamp(campService.toResponse(camp, user));
        return summary;
    }

    // --- Private helpers ---

    private CampRestSummaryResponse applyRest(UUID campaignId, CampSession camp, List<CampParticipant> targets,
                                              String restType, CampParticipantState successState, String username) {
        List<CampRestFailureResponse> failures = new ArrayList<>();
        int rested = 0;

        for (CampParticipant participant : targets) {
            UUID characterId = participant.getCharacter().getId();
            try {
                // Отдых и отметка «отдохнул» коммитятся вместе во вложенной транзакции.
                restExecutor.rest(campaignId, participant.getId(), characterId,
                        restType, successState, username);
                // Копии во внешнем контексте после этого устарели: без refresh ответ
                // ушёл бы с до-отдыховыми хитами и состоянием «отдыхает».
                entityManager.refresh(participant);
                entityManager.refresh(participant.getCharacter());
                rested++;
            } catch (Exception e) {
                String code = errorCode(e);
                participant.setState(CampParticipantState.FAILED);
                participant.setRestErrorCode(code);
                participant.setRestErrorMessage(truncate(e.getMessage()));
                campParticipantRepository.save(participant);
                failures.add(CampRestFailureResponse.builder()
                        .characterId(characterId)
                        .characterName(participant.getCharacter().getName())
                        .errorCode(code)
                        .errorMessage(truncate(e.getMessage()))
                        .build());
                log.warn("Camp rest failed for character: campId={}, characterId={}, code={}, reason={}",
                        camp.getId(), characterId, code, e.getMessage());
            }
        }

        int total = campParticipantRepository.findByCampSessionIdOrderByCreatedAtAsc(camp.getId()).size();
        return CampRestSummaryResponse.builder()
                .campId(camp.getId())
                .restType(restType)
                .partial(successState == CampParticipantState.PARTIAL)
                .restedCount(rested)
                .failedCount(failures.size())
                .skippedCount(Math.max(0, total - targets.size()))
                .failures(failures)
                .build();
    }

    private List<CampParticipant> selectTargets(UUID campId, List<UUID> characterIds,
                                                Set<CampParticipantState> eligibleStates) {
        List<CampParticipant> participants = campParticipantRepository
                .findByCampSessionIdOrderByCreatedAtAsc(campId);
        if (characterIds == null || characterIds.isEmpty()) {
            return participants.stream()
                    .filter(participant -> eligibleStates.contains(participant.getState()))
                    .toList();
        }

        Set<UUID> requested = new LinkedHashSet<>(characterIds);
        List<CampParticipant> targets = new ArrayList<>();
        for (UUID characterId : requested) {
            CampParticipant participant = participants.stream()
                    .filter(p -> p.getCharacter().getId().equals(characterId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Персонаж не участвует в привале: " + characterId));
            targets.add(participant);
        }
        return targets;
    }

    private String normalizeRestType(String input) {
        if (input == null) {
            throw new BadRequestException("Не указан тип отдыха");
        }
        return switch (input.toLowerCase()) {
            case "long", LONG_REST -> LONG_REST;
            case "short", SHORT_REST -> SHORT_REST;
            default -> throw new BadRequestException("Неизвестный тип отдыха: " + input);
        };
    }

    private String errorCode(Exception e) {
        if (e instanceof ConcurrencyFailureException) {
            return "REST_TX_CONFLICT";
        }
        if (e instanceof AccessDeniedException) {
            return "REST_FORBIDDEN";
        }
        if (e instanceof ResourceNotFoundException) {
            return "REST_TARGET_MISSING";
        }
        if (e instanceof BadRequestException) {
            return "REST_REJECTED";
        }
        return "REST_FAILED";
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
