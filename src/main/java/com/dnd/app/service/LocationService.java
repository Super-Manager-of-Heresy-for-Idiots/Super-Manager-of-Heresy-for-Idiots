package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.request.CreateLocationRequest;
import com.dnd.app.dto.request.UpdateLocationRequest;
import com.dnd.app.dto.response.LocationResponse;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CampaignLocationRepository;
import com.dnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Класс LocationService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private final CampaignLocationRepository locationRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final WebSocketEventService webSocketEventService;
    private final com.dnd.app.service.media.MediaUrlResolver mediaUrlResolver;

    /**
     * Создает результат операции "create location" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public LocationResponse createLocation(UUID campaignId, CreateLocationRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);

        CampaignLocation location = CampaignLocation.builder()
                .campaign(campaign)
                .name(request.getName())
                .description(request.getDescription())
                .isVisibleToPlayers(request.getIsVisibleToPlayers() != null ? request.getIsVisibleToPlayers() : false)
                .createdBy(user)
                .build();
        location = locationRepository.save(location);

        log.info("Location created: id={}, name='{}', campaignId={}, by={}", location.getId(), location.getName(), campaignId, username);
        return toResponse(location);
    }

    /**
     * Возвращает список для операции "list locations" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<LocationResponse> listLocations(UUID campaignId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);

        boolean isGm = isGmOrAdmin(campaignId, user);
        List<CampaignLocation> locations;
        if (isGm) {
            locations = locationRepository.findByCampaignId(campaignId);
        } else {
            locations = locationRepository.findByCampaignIdAndIsVisibleToPlayersTrue(campaignId);
        }
        return locations.stream().map(this::toResponse).toList();
    }

    /**
     * Возвращает результат операции "get location" в рамках бизнес-логики домена.
     * @param locationId идентификатор location, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public LocationResponse getLocation(UUID locationId, String username) {
        User user = getUser(username);
        CampaignLocation location = findLocation(locationId);
        campaignService.enforceMembershipOrAdmin(location.getCampaign(), user);

        boolean isGm = isGmOrAdmin(location.getCampaign().getId(), user);
        if (!isGm && !Boolean.TRUE.equals(location.getIsVisibleToPlayers())) {
            throw new ResourceNotFoundException("Location not found");
        }
        return toResponse(location);
    }

    /**
     * Обновляет результат операции "update location" в рамках бизнес-логики домена.
     * @param locationId идентификатор location, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public LocationResponse updateLocation(UUID locationId, UpdateLocationRequest request, String username) {
        User user = getUser(username);
        CampaignLocation location = findLocation(locationId);
        campaignService.enforceGmOrAdmin(location.getCampaign(), user);

        if (request.getName() != null) location.setName(request.getName());
        if (request.getDescription() != null) location.setDescription(request.getDescription());
        if (request.getIsVisibleToPlayers() != null) location.setIsVisibleToPlayers(request.getIsVisibleToPlayers());
        location = locationRepository.save(location);

        log.info("Location updated: id={}, by={}", locationId, username);
        return toResponse(location);
    }

    /**
     * Удаляет результат операции "delete location" в рамках бизнес-логики домена.
     * @param locationId идентификатор location, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void deleteLocation(UUID locationId, String username) {
        User user = getUser(username);
        CampaignLocation location = findLocation(locationId);
        campaignService.enforceGmOrAdmin(location.getCampaign(), user);

        locationRepository.delete(location);
        log.info("Location deleted: id={}, by={}", locationId, username);
    }

    /**
     * Преобразует данные операции "toggle visibility" в рамках бизнес-логики домена.
     * @param locationId идентификатор location, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public LocationResponse toggleVisibility(UUID locationId, String username) {
        User user = getUser(username);
        CampaignLocation location = findLocation(locationId);
        campaignService.enforceGmOrAdmin(location.getCampaign(), user);

        location.setIsVisibleToPlayers(!Boolean.TRUE.equals(location.getIsVisibleToPlayers()));
        location = locationRepository.save(location);

        log.info("Location visibility toggled: id={}, visible={}, by={}", locationId, location.getIsVisibleToPlayers(), username);

        boolean nowVisible = Boolean.TRUE.equals(location.getIsVisibleToPlayers());
        webSocketEventService.sendCampaignEvent(
                nowVisible ? WebSocketEventType.LOCATION_REVEALED : WebSocketEventType.LOCATION_HIDDEN,
                location.getCampaign().getId(), Map.of("locationId", locationId), user.getId());
        return toResponse(location);
    }

    // --- Private helpers ---

    private boolean isGmOrAdmin(UUID campaignId, User user) {
        return user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId());
    }

    private CampaignLocation findLocation(UUID locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found"));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private LocationResponse toResponse(CampaignLocation location) {
        return LocationResponse.builder()
                .id(location.getId())
                .name(location.getName())
                .description(location.getDescription())
                .isVisibleToPlayers(location.getIsVisibleToPlayers())
                .previewUrl(mediaUrlResolver.resolve(
                        com.dnd.app.domain.enums.MediaOwnerType.LOCATION_PREVIEW, location.getId(), null))
                .createdAt(location.getCreatedAt())
                .updatedAt(location.getUpdatedAt())
                .build();
    }
}
