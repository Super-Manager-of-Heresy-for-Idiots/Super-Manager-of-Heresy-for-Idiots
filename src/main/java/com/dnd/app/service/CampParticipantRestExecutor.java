package com.dnd.app.service;

import com.dnd.app.domain.CampParticipant;
import com.dnd.app.domain.enums.CampParticipantState;
import com.dnd.app.dto.response.RestResult;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CampParticipantRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс CampParticipantRestExecutor описывает шаг группового отдыха для одного персонажа.
 * Вынесен отдельным бином намеренно: отдых участника обязан идти в собственной транзакции
 * ({@code REQUIRES_NEW}), чтобы сбой одного персонажа не откатывал уже применённый отдых
 * остальных, — а через self-invocation прокси Spring такую границу не поставить.
 *
 * <p>В той же транзакции фиксируется и состояние участника: лист персонажа и отметка
 * «отдохнул» коммитятся вместе, поэтому сбой внешней транзакции не может оставить
 * применённый отдых с состоянием «отдыхает».
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampParticipantRestExecutor {

    private final RestOrchestrationService restOrchestrationService;
    private final CampParticipantRepository campParticipantRepository;
    private final ObjectMapper objectMapper;

    /**
     * Применяет отдых одному персонажу и фиксирует результат на участнике привала —
     * всё в одной отдельной транзакции.
     * @param campaignId идентификатор кампании
     * @param participantId идентификатор участника привала
     * @param characterId идентификатор персонажа
     * @param restType канонический код отдыха или сокращение API
     * @param successState состояние, присваиваемое участнику при успехе
     * @param username имя пользователя, от имени которого применяется отдых
     * @return результат отдыха персонажа
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RestResult rest(UUID campaignId, UUID participantId, UUID characterId,
                           String restType, CampParticipantState successState, String username) {
        RestResult result = restOrchestrationService.rest(campaignId, characterId, restType, username);

        CampParticipant participant = campParticipantRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Участник привала не найден"));
        participant.setState(successState);
        participant.setRestResultJson(writeRestResult(result, participantId));
        participant.setRestErrorCode(null);
        participant.setRestErrorMessage(null);
        participant.setRestedAt(Instant.now());
        campParticipantRepository.save(participant);

        return result;
    }

    private String writeRestResult(RestResult result, UUID participantId) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.warn("Camp rest result snapshot is not serializable: participantId={}, reason={}",
                    participantId, e.getMessage());
            return null;
        }
    }
}
