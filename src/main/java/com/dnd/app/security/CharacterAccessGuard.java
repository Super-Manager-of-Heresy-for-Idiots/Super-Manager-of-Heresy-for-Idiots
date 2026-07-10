package com.dnd.app.security;

import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.service.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Класс CharacterAccessGuard описывает компонент безопасности, который защищает бизнес-сценарии и проверяет доступ пользователя.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Component
@RequiredArgsConstructor
public class CharacterAccessGuard {

    private final PlayerCharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;

    /**
     * Проверяет требуемое условие операции "require" в рамках бизнес-логики безопасности.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public PlayerCharacter require(UUID characterId, String username) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        boolean owner = character.getOwner() != null && character.getOwner().getId().equals(user.getId());
        boolean gm = character.getCampaign() != null
                && campaignService.isGmInCampaign(character.getCampaign().getId(), user.getId());
        if (!owner && !gm && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Нет прав на этого персонажа");
        }
        return character;
    }
}
