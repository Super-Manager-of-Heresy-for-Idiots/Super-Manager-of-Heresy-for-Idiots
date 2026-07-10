package com.dnd.app.service;

import com.dnd.app.domain.CharacterFeat;
import com.dnd.app.domain.Feat;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.response.CharacterFeatResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CharacterFeatRepository;
import com.dnd.app.repository.FeatRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Класс CharacterFeatService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterFeatService {

    private final CharacterFeatRepository characterFeatRepository;
    private final PlayerCharacterRepository characterRepository;
    private final FeatRepository featRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final CharacterResourceService characterResourceService;

    /**
     * Возвращает список для операции "list" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<CharacterFeatResponse> list(UUID characterId, String username) {
        PlayerCharacter character = findCharacter(characterId);
        enforceView(character, getUser(username));
        return toResponses(characterFeatRepository.findByCharacterId(characterId));
    }

    /**
     * Добавляет результат операции "add" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param featId идентификатор feat, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public CharacterFeatResponse add(UUID characterId, UUID featId, String username) {
        PlayerCharacter character = findCharacter(characterId);
        enforceEdit(character, getUser(username));
        Feat feat = featRepository.findById(featId)
                .orElseThrow(() -> new ResourceNotFoundException("Фит не найден"));

        CharacterFeat existing = characterFeatRepository.findByCharacterIdAndFeatId(characterId, featId).orElse(null);
        if (existing != null) {
            return toResponse(existing, feat);
        }
        CharacterFeat saved = characterFeatRepository.save(CharacterFeat.builder()
                .characterId(characterId).featId(featId).source("manual").grantedAt(Instant.now()).build());
        characterResourceService.provisionFeatResources(character, List.of(featId));
        log.info("Feat {} added to character {} by {}", featId, characterId, username);
        return toResponse(saved, feat);
    }

    /**
     * Удаляет результат операции "remove" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param featId идентификатор feat, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void remove(UUID characterId, UUID featId, String username) {
        PlayerCharacter character = findCharacter(characterId);
        enforceEdit(character, getUser(username));
        // Feat-bound resource rows are left in place (they may hold spent state); a GM removes them manually.
        characterFeatRepository.findByCharacterIdAndFeatId(characterId, featId)
                .ifPresent(characterFeatRepository::delete);
    }

    /**
     * Выполняет операции "grant from source" в рамках бизнес-логики домена.
     * @param character входящее значение character, используемое бизнес-сценарием
     * @param featId идентификатор feat, используемый для выбора нужного бизнес-объекта
     * @param source входящее значение source, используемое бизнес-сценарием
     */
    @Transactional
    public void grantFromSource(PlayerCharacter character, UUID featId, String source) {
        if (featId == null || characterFeatRepository.existsByCharacterIdAndFeatId(character.getId(), featId)) {
            return;
        }
        try {
            characterFeatRepository.save(CharacterFeat.builder()
                    .characterId(character.getId()).featId(featId).source(source).grantedAt(Instant.now()).build());
            characterResourceService.provisionFeatResources(character, List.of(featId));
        } catch (RuntimeException e) {
            log.warn("Feat grant from source {} skipped for character {} (feat {}): {}",
                    source, character.getId(), featId, e.getMessage());
        }
    }

    private List<CharacterFeatResponse> toResponses(List<CharacterFeat> feats) {
        if (feats.isEmpty()) {
            return List.of();
        }
        Map<UUID, Feat> byId = featRepository.findAllById(feats.stream().map(CharacterFeat::getFeatId).toList())
                .stream().collect(Collectors.toMap(Feat::getId, f -> f));
        return feats.stream().map(cf -> toResponse(cf, byId.get(cf.getFeatId()))).toList();
    }

    private CharacterFeatResponse toResponse(CharacterFeat cf, Feat feat) {
        return CharacterFeatResponse.builder()
                .id(cf.getId()).featId(cf.getFeatId())
                .featName(feat != null ? feat.getNameRu() : null)
                .source(cf.getSource()).grantedAt(cf.getGrantedAt())
                .build();
    }

    private void enforceView(PlayerCharacter character, User user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (character.getCampaign() != null) {
            if (!campaignService.isMemberOfCampaign(character.getCampaign().getId(), user.getId())) {
                throw new AccessDeniedException("Вы не участник кампании этого персонажа");
            }
        } else if (character.getOwner() == null || !character.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Нет доступа к фитам этого персонажа");
        }
    }

    private void enforceEdit(PlayerCharacter character, User user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (character.getOwner() != null && character.getOwner().getId().equals(user.getId())) {
            return;
        }
        if (character.getCampaign() != null
                && campaignService.isGmInCampaign(character.getCampaign().getId(), user.getId())) {
            return;
        }
        throw new AccessDeniedException("Только владелец, ГМ кампании или ADMIN могут менять фиты");
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    private PlayerCharacter findCharacter(UUID characterId) {
        return characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
    }
}
