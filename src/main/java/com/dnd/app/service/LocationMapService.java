package com.dnd.app.service;

import com.dnd.app.domain.CampaignLocation;
import com.dnd.app.domain.LocationMap;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.AttachLocationMapRequest;
import com.dnd.app.dto.response.LocationMapResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CampaignLocationRepository;
import com.dnd.app.repository.LocationMapRepository;
import com.dnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Класс LocationMapService описывает привязку карт map-service к локациям кампании
 * (WORLD_PLAN Этап 4): ГМ прикрепляет карты к локации и назначает карту по умолчанию,
 * которая предлагается при создании боя в этой локации.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationMapService {

    private final LocationMapRepository locationMapRepository;
    private final CampaignLocationRepository locationRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;

    /**
     * Возвращает карты, привязанные к локации (участники кампании; для игроков —
     * только если локация видима).
     * @param campaignId идентификатор кампании
     * @param locationId идентификатор локации
     * @param username имя пользователя, выполняющего запрос
     * @return список привязок карт
     */
    @Transactional(readOnly = true)
    public List<LocationMapResponse> listMaps(UUID campaignId, UUID locationId, String username) {
        User user = getUser(username);
        CampaignLocation location = findLocationInCampaign(locationId, campaignId);
        campaignService.enforceMembershipOrAdmin(location.getCampaign(), user);
        boolean gm = isGmOrAdmin(campaignId, user);
        if (!gm && !Boolean.TRUE.equals(location.getIsVisibleToPlayers())) {
            throw new ResourceNotFoundException("Location not found");
        }
        return locationMapRepository.findByLocationId(locationId).stream().map(this::toResponse).toList();
    }

    /**
     * Привязывает карту map-service к локации (GM only). Повторная привязка той же
     * карты обновляет флаг is_default. Назначение default снимает флаг с других карт.
     * @param campaignId идентификатор кампании
     * @param locationId идентификатор локации
     * @param request входящие данные запроса
     * @param username имя пользователя, выполняющего действие
     * @return созданная/обновлённая привязка
     */
    @Transactional
    public LocationMapResponse attachMap(UUID campaignId, UUID locationId, AttachLocationMapRequest request, String username) {
        User user = getUser(username);
        CampaignLocation location = findLocationInCampaign(locationId, campaignId);
        campaignService.enforceGmOrAdmin(location.getCampaign(), user);

        boolean makeDefault = Boolean.TRUE.equals(request.getIsDefault());
        LocationMap link = locationMapRepository
                .findByLocationIdAndExternalMapId(locationId, request.getExternalMapId())
                .orElseGet(() -> LocationMap.builder()
                        .location(location)
                        .externalMapId(request.getExternalMapId())
                        .build());
        if (makeDefault) {
            clearDefault(locationId);
        }
        link.setIsDefault(makeDefault);
        link = locationMapRepository.save(link);

        log.info("Map attached to location: locationId={}, mapId={}, default={}, by={}",
                locationId, request.getExternalMapId(), makeDefault, username);
        return toResponse(link);
    }

    /**
     * Отвязывает карту от локации (GM only).
     * @param campaignId идентификатор кампании
     * @param locationId идентификатор локации
     * @param linkId идентификатор привязки
     * @param username имя пользователя, выполняющего действие
     */
    @Transactional
    public void detachMap(UUID campaignId, UUID locationId, UUID linkId, String username) {
        User user = getUser(username);
        CampaignLocation location = findLocationInCampaign(locationId, campaignId);
        campaignService.enforceGmOrAdmin(location.getCampaign(), user);

        LocationMap link = locationMapRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Map link not found"));
        if (!link.getLocation().getId().equals(locationId)) {
            throw new BadRequestException("Map link does not belong to that location");
        }
        locationMapRepository.delete(link);
        log.info("Map detached from location: locationId={}, linkId={}, by={}", locationId, linkId, username);
    }

    // --- Private helpers ---

    private void clearDefault(UUID locationId) {
        List<LocationMap> defaults = locationMapRepository.findByLocationIdAndIsDefaultTrue(locationId);
        for (LocationMap other : defaults) {
            other.setIsDefault(false);
        }
        locationMapRepository.saveAll(defaults);
    }

    private CampaignLocation findLocationInCampaign(UUID locationId, UUID campaignId) {
        CampaignLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found"));
        if (location.getCampaign() == null || !location.getCampaign().getId().equals(campaignId)) {
            throw new ResourceNotFoundException("Location not found in this campaign");
        }
        return location;
    }

    private boolean isGmOrAdmin(UUID campaignId, User user) {
        return user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId());
    }

    private LocationMapResponse toResponse(LocationMap link) {
        return LocationMapResponse.builder()
                .id(link.getId())
                .locationId(link.getLocation().getId())
                .externalMapId(link.getExternalMapId())
                .isDefault(link.getIsDefault())
                .createdAt(link.getCreatedAt())
                .build();
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
