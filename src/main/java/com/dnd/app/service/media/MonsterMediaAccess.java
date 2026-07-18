package com.dnd.app.service.media;

import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.Monster;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.MonsterRepository;
import com.dnd.app.service.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Класс MonsterMediaAccess — общая scope-aware проверка прав на изображения монстра (портрет и токен).
 * Монстр может быть системным (правит только ADMIN), homebrew (правит автор пакета) или кампанийным
 * (правит GM кампании). Обе политики монстра ({@link MonsterPortraitPolicy}, {@link MonsterTokenPolicy})
 * делегируют сюда, чтобы не дублировать логику доступа.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "minio.endpoint")
class MonsterMediaAccess {

    private final MonsterRepository monsterRepository;
    private final CampaignService campaignService;

    /**
     * Проверяет право менять изображение монстра по его области видимости (scope).
     * @param monsterId идентификатор монстра
     * @param user текущий пользователь
     */
    void checkEdit(UUID monsterId, MediaUser user) {
        Monster monster = requireMonster(monsterId);
        if (user.isAdmin()) {
            return;
        }
        if (monster.getCampaign() != null) {
            if (!campaignService.isGmInCampaign(monster.getCampaign().getId(), user.id())) {
                throw new AccessDeniedException("Изображение монстра кампании может менять только GM.");
            }
        } else if (monster.getHomebrew() != null) {
            HomebrewPackage pkg = monster.getHomebrew();
            if (pkg.getAuthor() == null || !pkg.getAuthor().getId().equals(user.id())) {
                throw new AccessDeniedException("Изображение homebrew-монстра может менять только автор пакета.");
            }
        } else {
            throw new AccessDeniedException("Системный бестиарий может редактировать только администратор.");
        }
    }

    /**
     * Проверяет право видеть изображение монстра по его области видимости (scope).
     * @param monsterId идентификатор монстра
     * @param user текущий пользователь
     */
    void checkView(UUID monsterId, MediaUser user) {
        Monster monster = requireMonster(monsterId);
        if (user.isAdmin()) {
            return;
        }
        if (monster.getCampaign() != null) {
            UUID campaignId = monster.getCampaign().getId();
            boolean member = campaignService.isMemberOfCampaign(campaignId, user.id());
            boolean gm = campaignService.isGmInCampaign(campaignId, user.id());
            if (!(member && (gm || Boolean.TRUE.equals(monster.getIsVisibleToPlayers())))) {
                throw new AccessDeniedException("Монстр доступен только участникам кампании.");
            }
        } else if (monster.getHomebrew() != null) {
            HomebrewPackage pkg = monster.getHomebrew();
            boolean readable = (pkg.getAuthor() != null && pkg.getAuthor().getId().equals(user.id()))
                    || (pkg.getStatus() == HomebrewStatus.PUBLISHED && !pkg.isDeleted());
            if (!readable) {
                throw new AccessDeniedException("Монстр из недоступного homebrew-пакета.");
            }
        }
        // SYSTEM-монстр (без кампании и homebrew) виден любому аутентифицированному.
    }

    /**
     * Находит монстра или бросает 404.
     * @param id идентификатор монстра
     * @return сущность монстра
     */
    private Monster requireMonster(UUID id) {
        return monsterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Монстр не найден."));
    }
}
