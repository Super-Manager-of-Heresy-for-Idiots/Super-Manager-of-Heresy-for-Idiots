package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.ContentType;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.ActivateHomebrewRequest;
import com.dnd.app.dto.response.TeamAvailableContentResponse;
import com.dnd.app.dto.response.TeamAvailableContentResponse.AvailableContentItem;
import com.dnd.app.dto.response.TeamHomebrewActivationResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamContentService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamHomebrewActivationRepository activationRepository;
    private final HomebrewPackageRepository homebrewPackageRepository;
    private final HomebrewContentItemRepository contentItemRepository;
    private final CharacterClassRepository classRepository;
    private final CharacterRaceRepository raceRepository;
    private final ItemTypeRepository itemTypeRepository;
    private final SkillRepository skillRepository;
    private final FeatRepository featRepository;
    private final UserRepository userRepository;

    @Transactional
    public TeamHomebrewActivationResponse activateHomebrew(UUID teamId, ActivateHomebrewRequest request, String username) {
        User user = getUser(username);
        Team team = getTeam(teamId);
        enforceTeamOwnerOrAdmin(team, user);

        HomebrewPackage pkg = homebrewPackageRepository.findById(request.getHomebrewPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Homebrew-пакет не найден"));

        if (pkg.getStatus() != HomebrewStatus.PUBLISHED || pkg.isDeleted()) {
            throw new ResourceNotFoundException("Homebrew-пакет не найден или не опубликован");
        }

        if (activationRepository.existsByTeamIdAndHomebrewPackageId(teamId, pkg.getId())) {
            throw new DuplicateResourceException("Этот пакет уже активирован для данной команды");
        }

        TeamHomebrewActivation activation = TeamHomebrewActivation.builder()
                .team(team)
                .homebrewPackage(pkg)
                .build();
        activation = activationRepository.save(activation);

        log.info("Homebrew activated: package='{}', teamId={}, by user={}", pkg.getTitle(), teamId, username);

        return buildActivationResponse(activation);
    }

    @Transactional
    public void deactivateHomebrew(UUID teamId, UUID homebrewPackageId, String username) {
        User user = getUser(username);
        Team team = getTeam(teamId);
        enforceTeamOwnerOrAdmin(team, user);

        TeamHomebrewActivation activation = activationRepository.findByTeamIdAndHomebrewPackageId(teamId, homebrewPackageId)
                .orElseThrow(() -> new ResourceNotFoundException("Активация пакета не найдена для этой команды"));

        activationRepository.delete(activation);
        log.info("Homebrew deactivated: packageId={}, teamId={}, by user={}", homebrewPackageId, teamId, username);
    }

    @Transactional(readOnly = true)
    public List<TeamHomebrewActivationResponse> listActiveHomebrew(UUID teamId, String username) {
        User user = getUser(username);
        Team team = getTeam(teamId);
        enforceTeamAccess(team, user);

        List<TeamHomebrewActivation> activations = activationRepository.findAllByTeamId(teamId);
        return activations.stream().map(this::buildActivationResponse).toList();
    }

    @Transactional(readOnly = true)
    public TeamAvailableContentResponse getAvailableContent(UUID teamId, String username) {
        User user = getUser(username);
        Team team = getTeam(teamId);
        enforceTeamAccess(team, user);

        Set<UUID> activePackageIds = activationRepository.findPackageIdsByTeamId(teamId);

        List<AvailableContentItem> classes = new ArrayList<>();
        classRepository.findAllBySourceHomebrewIsNull().forEach(c ->
                classes.add(AvailableContentItem.builder()
                        .id(c.getId()).name(c.getName()).source("GLOBAL").build()));
        if (!activePackageIds.isEmpty()) {
            classRepository.findAllBySourceHomebrewIdIn(activePackageIds).forEach(c ->
                    classes.add(AvailableContentItem.builder()
                            .id(c.getId()).name(c.getName()).source("HOMEBREW")
                            .homebrewTitle(c.getSourceHomebrew() != null ? c.getSourceHomebrew().getTitle() : null)
                            .build()));
        }

        List<AvailableContentItem> races = raceRepository.findAll().stream()
                .map(r -> AvailableContentItem.builder()
                        .id(r.getId()).name(r.getName()).source("GLOBAL").build())
                .toList();

        List<AvailableContentItem> itemTypes = new ArrayList<>();
        itemTypeRepository.findAllBySourceHomebrewIsNull().forEach(it ->
                itemTypes.add(AvailableContentItem.builder()
                        .id(it.getId()).name(it.getName()).source("GLOBAL").build()));
        if (!activePackageIds.isEmpty()) {
            itemTypeRepository.findAllBySourceHomebrewIdIn(activePackageIds).forEach(it ->
                    itemTypes.add(AvailableContentItem.builder()
                            .id(it.getId()).name(it.getName()).source("HOMEBREW")
                            .homebrewTitle(it.getSourceHomebrew() != null ? it.getSourceHomebrew().getTitle() : null)
                            .build()));
        }

        List<AvailableContentItem> skills = new ArrayList<>();
        skillRepository.findAllBySourceHomebrewIsNull().forEach(s ->
                skills.add(AvailableContentItem.builder()
                        .id(s.getId()).name(s.getName()).source("GLOBAL").build()));
        if (!activePackageIds.isEmpty()) {
            skillRepository.findAllBySourceHomebrewIdIn(activePackageIds).forEach(s ->
                    skills.add(AvailableContentItem.builder()
                            .id(s.getId()).name(s.getName()).source("HOMEBREW")
                            .homebrewTitle(s.getSourceHomebrew() != null ? s.getSourceHomebrew().getTitle() : null)
                            .build()));
        }

        List<AvailableContentItem> feats = new ArrayList<>();
        featRepository.findAllBySourceHomebrewIsNull().forEach(f ->
                feats.add(AvailableContentItem.builder()
                        .id(f.getId()).name(f.getName()).source("GLOBAL").build()));
        if (!activePackageIds.isEmpty()) {
            featRepository.findAllBySourceHomebrewIdIn(activePackageIds).forEach(f ->
                    feats.add(AvailableContentItem.builder()
                            .id(f.getId()).name(f.getName()).source("HOMEBREW")
                            .homebrewTitle(f.getSourceHomebrew() != null ? f.getSourceHomebrew().getTitle() : null)
                            .build()));
        }

        return TeamAvailableContentResponse.builder()
                .classes(classes).races(races).itemTypes(itemTypes).skills(skills).feats(feats)
                .build();
    }

    public boolean isClassAvailableInTeam(UUID teamId, UUID classId) {
        CharacterClass cc = classRepository.findById(classId).orElse(null);
        if (cc == null) return false;
        if (cc.getSourceHomebrew() == null) return true;
        Set<UUID> activePackageIds = activationRepository.findPackageIdsByTeamId(teamId);
        return activePackageIds.contains(cc.getSourceHomebrew().getId());
    }

    public boolean isRaceAvailableInTeam(UUID teamId, UUID raceId) {
        return raceRepository.existsById(raceId);
    }

    private TeamHomebrewActivationResponse buildActivationResponse(TeamHomebrewActivation activation) {
        HomebrewPackage pkg = activation.getHomebrewPackage();
        List<Object[]> countsByType = contentItemRepository.countByPackageGroupedByType(pkg.getId());
        Map<String, Long> contentSummary = new LinkedHashMap<>();
        for (Object[] row : countsByType) {
            ContentType ct = (ContentType) row[0];
            Long count = (Long) row[1];
            contentSummary.put(ct.name().toLowerCase() + "Count", count);
        }
        return TeamHomebrewActivationResponse.builder()
                .homebrewPackageId(pkg.getId()).title(pkg.getTitle())
                .contentSummary(contentSummary).activatedAt(activation.getActivatedAt())
                .build();
    }

    private void enforceTeamOwnerOrAdmin(Team team, User user) {
        if (user.getRole() == Role.ADMIN) return;
        if (user.getRole() == Role.GAME_MASTER && team.getGameMaster().getId().equals(user.getId())) return;
        throw new AccessDeniedException("Только владелец команды или администратор может выполнить это действие");
    }

    private void enforceTeamAccess(Team team, User user) {
        if (user.getRole() == Role.ADMIN) return;
        if (user.getRole() == Role.GAME_MASTER && team.getGameMaster().getId().equals(user.getId())) return;
        if (user.getRole() == Role.PLAYER) {
            boolean isMember = teamMemberRepository.existsByIdTeamIdAndIdPlayerId(team.getId(), user.getId());
            if (isMember) return;
        }
        throw new AccessDeniedException("Нет доступа к этой команде");
    }

    private Team getTeam(UUID teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Команда не найдена"));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }
}
