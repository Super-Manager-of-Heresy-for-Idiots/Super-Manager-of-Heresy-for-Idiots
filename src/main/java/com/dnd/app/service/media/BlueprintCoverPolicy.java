package com.dnd.app.service.media;

import com.dnd.app.domain.CampaignBlueprint;
import com.dnd.app.domain.enums.BlueprintStatus;
import com.dnd.app.domain.enums.MediaOwnerType;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CampaignBlueprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Класс BlueprintCoverPolicy — политика прав на обложку campaign blueprint ({@code BLUEPRINT_COVER}).
 * Загрузку/замену/удаление разрешает автору шаблона (или ADMIN) и только пока шаблон в статусе
 * черновика (DRAFT) — так же строго, как {@code CampaignBlueprintService.getEditableBlueprint}
 * (шаблоны строже homebrew: PUBLISHED уже не редактируется). Чтение — любому аутентифицированному
 * (обложка видна на витрине шаблонов).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "minio.endpoint")
public class BlueprintCoverPolicy implements MediaOwnerPolicy {

    private final CampaignBlueprintRepository blueprintRepository;

    /** @return тип владельца, обслуживаемый политикой — обложка blueprint */
    @Override
    public MediaOwnerType type() {
        return MediaOwnerType.BLUEPRINT_COVER;
    }

    /**
     * Разрешает менять обложку автору шаблона (или ADMIN) только в статусе DRAFT.
     * @param ownerId идентификатор blueprint
     * @param user текущий пользователь
     */
    @Override
    public void checkUpload(UUID ownerId, MediaUser user) {
        CampaignBlueprint blueprint = requireBlueprint(ownerId);
        boolean ownerOrAdmin = user.isAdmin()
                || (blueprint.getAuthor() != null && blueprint.getAuthor().getId().equals(user.id()));
        if (!ownerOrAdmin) {
            throw new AccessDeniedException("Обложку шаблона может менять только его автор.");
        }
        if (blueprint.getStatus() != BlueprintStatus.DRAFT) {
            throw new BadRequestException("Обложку можно менять только в черновике шаблона (DRAFT).");
        }
    }

    /**
     * Разрешает чтение обложки любому аутентифицированному пользователю (витрина шаблонов).
     * @param ownerId идентификатор blueprint
     * @param user текущий пользователь
     */
    @Override
    public void checkRead(UUID ownerId, MediaUser user) {
        // Обложка шаблона видна любому аутентифицированному пользователю — дополнительных проверок нет.
    }

    /**
     * Находит не удалённый blueprint или бросает 404.
     * @param id идентификатор blueprint
     * @return сущность blueprint
     */
    private CampaignBlueprint requireBlueprint(UUID id) {
        CampaignBlueprint blueprint = blueprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Шаблон кампании не найден."));
        if (blueprint.isDeleted()) {
            throw new ResourceNotFoundException("Шаблон кампании не найден.");
        }
        return blueprint;
    }
}
