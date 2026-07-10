package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.CreateItemTemplateRequest;
import com.dnd.app.dto.response.ItemTemplateResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Класс ItemTemplateService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemTemplateService {

    private final ItemTemplateRepository itemTemplateRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final ContentDictionaryResolver contentDictionaryResolver;

    /**
     * Создает результат операции "create template" в рамках бизнес-логики домена.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public ItemTemplateResponse createTemplate(CreateItemTemplateRequest request, String username) {
        User user = getUser(username);
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.GAME_MASTER) {
            throw new AccessDeniedException("Only ADMIN or GM can create item templates");
        }

        Rarity rarity = contentDictionaryResolver.resolveRarity(request.getRarity(), null);
        DamageType damageType = contentDictionaryResolver.resolveDamageType(request.getDamageType(), null);

        ItemTemplate template = ItemTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .rarity(rarity)
                .damageDice(request.getDamageDice())
                .damageBonus(request.getDamageBonus() != null ? request.getDamageBonus() : 0)
                .damageType(damageType)
                .isStackable(request.getIsStackable() != null ? request.getIsStackable() : false)
                .build();
        template = itemTemplateRepository.save(template);

        log.info("Item template created: id={}, name='{}', by={}", template.getId(), template.getName(), username);
        return toResponse(template);
    }

    /**
     * Возвращает результат операции "get template" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public ItemTemplateResponse getTemplate(UUID id) {
        ItemTemplate template = itemTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item template not found"));
        return toResponse(template);
    }

    /**
     * Возвращает список для операции "list all vanilla" в рамках бизнес-логики домена.
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<ItemTemplateResponse> listAllVanilla(String username) {
        User user = getUser(username);
        if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can list all item templates");
        }
        return itemTemplateRepository.findByHomebrewIsNull().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Возвращает список для операции "list templates" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<ItemTemplateResponse> listTemplates(UUID campaignId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);

        // Vanilla templates (no homebrew)
        List<ItemTemplate> vanilla = itemTemplateRepository.findByHomebrewIsNull();

        // Campaign homebrew templates
        List<UUID> packageIds = campaignService.findCampaign(campaignId).getMembers().stream()
                .map(m -> m.getCampaign().getId())
                .toList();
        // Actually get homebrew package IDs from campaign_homebrew
        List<ItemTemplate> all = new ArrayList<>(vanilla);

        List<UUID> homebrewIds = getHomebrewPackageIds(campaignId);
        if (!homebrewIds.isEmpty()) {
            all.addAll(itemTemplateRepository.findByHomebrewIdIn(homebrewIds));
        }

        return all.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Обновляет результат операции "update template" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public ItemTemplateResponse updateTemplate(UUID id, CreateItemTemplateRequest request, String username) {
        User user = getUser(username);
        ItemTemplate template = itemTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item template not found"));

        // ADMIN can update vanilla, author GM can update homebrew
        if (template.getHomebrew() == null) {
            if (user.getRole() != Role.ADMIN) {
                throw new AccessDeniedException("Only ADMIN can update vanilla item templates");
            }
        } else {
            if (user.getRole() != Role.ADMIN
                    && !template.getHomebrew().getAuthor().getId().equals(user.getId())) {
                throw new AccessDeniedException("Only the author GM can update homebrew item templates");
            }
        }

        if (request.getName() != null) template.setName(request.getName());
        if (request.getDescription() != null) template.setDescription(request.getDescription());
        if (request.getRarity() != null) template.setRarity(contentDictionaryResolver.resolveRarity(request.getRarity(), template.getHomebrew()));
        if (request.getDamageDice() != null) template.setDamageDice(request.getDamageDice());
        if (request.getDamageBonus() != null) template.setDamageBonus(request.getDamageBonus());
        if (request.getDamageType() != null) template.setDamageType(contentDictionaryResolver.resolveDamageType(request.getDamageType(), template.getHomebrew()));
        if (request.getIsStackable() != null) template.setIsStackable(request.getIsStackable());

        template = itemTemplateRepository.save(template);
        log.info("Item template updated: id={}, by={}", id, username);
        return toResponse(template);
    }

    /**
     * Удаляет результат операции "delete template" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void deleteTemplate(UUID id, String username) {
        User user = getUser(username);
        if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN can delete vanilla item templates");
        }

        ItemTemplate template = itemTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item template not found"));
        itemTemplateRepository.delete(template);
        log.info("Item template deleted: id={}, by={}", id, username);
    }

    // --- Private helpers ---

    private List<UUID> getHomebrewPackageIds(UUID campaignId) {
        // This is a simplified approach; ideally ContentScopeService would provide this
        return List.of();
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ItemTemplateResponse toResponse(ItemTemplate template) {
        return ItemTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .rarity(template.getRarity() != null ? template.getRarity().getSlug() : null)
                .damageDice(template.getDamageDice())
                .damageBonus(template.getDamageBonus())
                .damageType(template.getDamageType() != null ? template.getDamageType().getSlug() : null)
                .isStackable(template.getIsStackable())
                .sourceHomebrewTitle(template.getHomebrew() != null ? template.getHomebrew().getTitle() : null)
                .build();
    }
}
