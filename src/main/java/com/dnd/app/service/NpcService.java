package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.enums.NpcSourceType;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.request.CreateNoteRequest;
import com.dnd.app.dto.request.CreateNpcRequest;
import com.dnd.app.dto.request.UpdateNoteRequest;
import com.dnd.app.dto.request.UpdateNpcRequest;
import com.dnd.app.dto.response.NoteResponse;
import com.dnd.app.dto.response.NpcResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CampaignHomebrewRepository;
import com.dnd.app.repository.CampaignNpcRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.SpeciesRepository;
import com.dnd.app.repository.MonsterRepository;
import com.dnd.app.repository.NpcNoteRepository;
import com.dnd.app.repository.SpellRepository;
import com.dnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NpcService {

    private final CampaignNpcRepository npcRepository;
    private final NpcNoteRepository noteRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final WebSocketEventService webSocketEventService;
    private final SpeciesRepository speciesRepository;
    private final ContentCharacterClassRepository classRepository;
    private final SpellRepository spellRepository;
    private final MonsterRepository monsterRepository;
    private final CampaignHomebrewRepository campaignHomebrewRepository;

    @Transactional
    public NpcResponse createNpc(UUID campaignId, CreateNpcRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);

        CampaignNpc npc = CampaignNpc.builder()
                .campaign(campaign)
                .name(request.getName())
                .publicDescription(request.getPublicDescription())
                .privateDescription(request.getPrivateDescription())
                .isVisibleToPlayers(request.getIsVisibleToPlayers() != null ? request.getIsVisibleToPlayers() : false)
                .createdBy(user)
                .build();

        applySource(npc, campaign, request.getSourceType(),
                request.getRaceId(), request.getClassId(), request.getLevel(),
                request.getAbilities(), request.getSpellIds(), request.getSourceMonsterId());

        npc = npcRepository.save(npc);

        log.info("NPC created: id={}, name='{}', source={}, campaignId={}, by={}",
                npc.getId(), npc.getName(), npc.getSourceType(), campaignId, username);
        return toResponse(npc, true);
    }

    @Transactional(readOnly = true)
    public List<NpcResponse> listNpcs(UUID campaignId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);

        boolean isGm = isGmOrAdmin(campaignId, user);
        List<CampaignNpc> npcs;
        if (isGm) {
            npcs = npcRepository.findByCampaignId(campaignId);
        } else {
            npcs = npcRepository.findByCampaignIdAndIsVisibleToPlayersTrue(campaignId);
        }
        return npcs.stream().map(npc -> toResponse(npc, isGm)).toList();
    }

    @Transactional(readOnly = true)
    public NpcResponse getNpc(UUID npcId, String username) {
        User user = getUser(username);
        CampaignNpc npc = findNpc(npcId);
        campaignService.enforceMembershipOrAdmin(npc.getCampaign(), user);

        boolean isGm = isGmOrAdmin(npc.getCampaign().getId(), user);
        if (!isGm && !Boolean.TRUE.equals(npc.getIsVisibleToPlayers())) {
            throw new ResourceNotFoundException("NPC not found");
        }
        return toResponse(npc, isGm);
    }

    @Transactional
    public NpcResponse updateNpc(UUID npcId, UpdateNpcRequest request, String username) {
        User user = getUser(username);
        CampaignNpc npc = findNpc(npcId);
        campaignService.enforceGmOrAdmin(npc.getCampaign(), user);

        if (request.getName() != null) npc.setName(request.getName());
        if (request.getPublicDescription() != null) npc.setPublicDescription(request.getPublicDescription());
        if (request.getPrivateDescription() != null) npc.setPrivateDescription(request.getPrivateDescription());
        if (request.getIsVisibleToPlayers() != null) npc.setIsVisibleToPlayers(request.getIsVisibleToPlayers());

        applyUpdateSource(npc, request);

        npc = npcRepository.save(npc);

        log.info("NPC updated: id={}, by={}", npcId, username);
        return toResponse(npc, true);
    }

    @Transactional
    public void deleteNpc(UUID npcId, String username) {
        User user = getUser(username);
        CampaignNpc npc = findNpc(npcId);
        campaignService.enforceGmOrAdmin(npc.getCampaign(), user);

        npcRepository.delete(npc);
        log.info("NPC deleted: id={}, by={}", npcId, username);
    }

    @Transactional
    public NpcResponse toggleVisibility(UUID npcId, String username) {
        User user = getUser(username);
        CampaignNpc npc = findNpc(npcId);
        campaignService.enforceGmOrAdmin(npc.getCampaign(), user);

        npc.setIsVisibleToPlayers(!Boolean.TRUE.equals(npc.getIsVisibleToPlayers()));
        npc = npcRepository.save(npc);

        log.info("NPC visibility toggled: id={}, visible={}, by={}", npcId, npc.getIsVisibleToPlayers(), username);

        boolean nowVisible = Boolean.TRUE.equals(npc.getIsVisibleToPlayers());
        webSocketEventService.sendCampaignEvent(
                nowVisible ? WebSocketEventType.NPC_REVEALED : WebSocketEventType.NPC_HIDDEN,
                npc.getCampaign().getId(), Map.of("npcId", npcId), user.getId());
        return toResponse(npc, true);
    }

    // --- Notes ---

    @Transactional
    public NoteResponse addNote(UUID npcId, CreateNoteRequest request, String username) {
        User user = getUser(username);
        CampaignNpc npc = findNpc(npcId);

        boolean isGm = isGmOrAdmin(npc.getCampaign().getId(), user);
        if (!isGm) {
            campaignService.enforceMembershipOrAdmin(npc.getCampaign(), user);
            if (!Boolean.TRUE.equals(npc.getIsVisibleToPlayers())) {
                throw new ResourceNotFoundException("NPC not found");
            }
        }

        NpcNote note = NpcNote.builder()
                .npc(npc)
                .author(user)
                .content(request.getContent())
                .build();
        note = noteRepository.save(note);

        log.info("NPC note added: noteId={}, npcId={}, by={}", note.getId(), npcId, username);
        return toNoteResponse(note);
    }

    @Transactional
    public NoteResponse updateNote(UUID noteId, UpdateNoteRequest request, String username) {
        User user = getUser(username);
        NpcNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        if (!note.getAuthor().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only the note author can update it");
        }

        if (request.getContent() != null) note.setContent(request.getContent());
        note = noteRepository.save(note);

        log.info("NPC note updated: noteId={}, by={}", noteId, username);
        return toNoteResponse(note);
    }

    @Transactional
    public void deleteNote(UUID noteId, String username) {
        User user = getUser(username);
        NpcNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found"));

        boolean isGm = isGmOrAdmin(note.getNpc().getCampaign().getId(), user);
        if (!note.getAuthor().getId().equals(user.getId()) && !isGm) {
            throw new AccessDeniedException("Only the note author or a GM can delete it");
        }

        noteRepository.delete(note);
        log.info("NPC note deleted: noteId={}, by={}", noteId, username);
    }

    // --- Private helpers ---

    private boolean isGmOrAdmin(UUID campaignId, User user) {
        return user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId());
    }

    private CampaignNpc findNpc(UUID npcId) {
        return npcRepository.findById(npcId)
                .orElseThrow(() -> new ResourceNotFoundException("NPC not found"));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private NpcResponse toResponse(CampaignNpc npc, boolean includePrivate) {
        List<NoteResponse> notes = npc.getNotes().stream()
                .map(this::toNoteResponse)
                .toList();

        List<NpcResponse.Ref> spellRefs = npc.getSpells().isEmpty() ? null
                : npc.getSpells().stream()
                        .map(s -> ref(s.getId(), s.getNameRu()))
                        .toList();

        return NpcResponse.builder()
                .id(npc.getId())
                .name(npc.getName())
                .publicDescription(npc.getPublicDescription())
                .privateDescription(includePrivate ? npc.getPrivateDescription() : null)
                .isVisibleToPlayers(npc.getIsVisibleToPlayers())
                .sourceType(npc.getSourceType())
                .race(npc.getRace() == null ? null
                        : ref(npc.getRace().getId(), displayName(npc.getRace().getNameRu(), npc.getRace().getNameEn())))
                .characterClass(npc.getCharacterClass() == null ? null
                        : ref(npc.getCharacterClass().getId(),
                              displayName(npc.getCharacterClass().getNameRu(), npc.getCharacterClass().getNameEn())))
                .level(npc.getLevel())
                .abilities(npc.getAbilities())
                .spells(spellRefs)
                .sourceMonster(npc.getSourceMonster() == null ? null
                        : ref(npc.getSourceMonster().getId(), npc.getSourceMonster().getNameRusloc()))
                .notes(notes)
                .createdAt(npc.getCreatedAt())
                .updatedAt(npc.getUpdatedAt())
                .build();
    }

    private NpcResponse.Ref ref(UUID id, String name) {
        return NpcResponse.Ref.builder().id(id).name(name).build();
    }

    private String displayName(String localized, String fallback) {
        return localized != null && !localized.isBlank() ? localized : fallback;
    }

    // --- NPC source/build application & validation ---

    private void applySource(CampaignNpc npc, Campaign campaign, NpcSourceType sourceType,
                             UUID raceId, UUID classId, Integer level, String abilities,
                             List<UUID> spellIds, UUID sourceMonsterId) {
        npc.setSourceType(sourceType);
        if (sourceType == NpcSourceType.CLASS_BASED) {
            // Required: race, class, level. Everything else is optional and is NOT
            // validated against level/class — the GM authors abilities freely.
            npc.setRace(resolveRace(requireField(raceId, "raceId is required for a class-based NPC")));
            npc.setCharacterClass(resolveClass(requireField(classId, "classId is required for a class-based NPC")));
            npc.setLevel(requireField(level, "level is required for a class-based NPC"));
            npc.setAbilities(abilities);
            npc.getSpells().clear();
            npc.getSpells().addAll(resolveSpells(spellIds));
        } else if (sourceType == NpcSourceType.MONSTER_BASED) {
            UUID monsterId = requireField(sourceMonsterId, "sourceMonsterId is required for a monster-based NPC");
            npc.setSourceMonster(resolveCampaignMonster(monsterId, campaign));
        }
        // sourceType == null => legacy free-form NPC (no build).
    }

    private void applyUpdateSource(CampaignNpc npc, UpdateNpcRequest request) {
        NpcSourceType targetType = request.getSourceType() != null ? request.getSourceType() : npc.getSourceType();
        boolean switching = request.getSourceType() != null && request.getSourceType() != npc.getSourceType();

        if (targetType == NpcSourceType.CLASS_BASED) {
            npc.setSourceType(NpcSourceType.CLASS_BASED);
            npc.setSourceMonster(null);
            if (request.getRaceId() != null) npc.setRace(resolveRace(request.getRaceId()));
            if (request.getClassId() != null) npc.setCharacterClass(resolveClass(request.getClassId()));
            if (request.getLevel() != null) npc.setLevel(request.getLevel());
            if (request.getAbilities() != null) npc.setAbilities(request.getAbilities());
            if (request.getSpellIds() != null) {
                npc.getSpells().clear();
                npc.getSpells().addAll(resolveSpells(request.getSpellIds()));
            }
            if (switching) {
                requireField(npc.getRace(), "raceId is required for a class-based NPC");
                requireField(npc.getCharacterClass(), "classId is required for a class-based NPC");
                requireField(npc.getLevel(), "level is required for a class-based NPC");
            }
        } else if (targetType == NpcSourceType.MONSTER_BASED) {
            npc.setSourceType(NpcSourceType.MONSTER_BASED);
            if (switching) {
                npc.setRace(null);
                npc.setCharacterClass(null);
                npc.setLevel(null);
                npc.setAbilities(null);
                npc.getSpells().clear();
            }
            if (request.getSourceMonsterId() != null) {
                npc.setSourceMonster(resolveCampaignMonster(request.getSourceMonsterId(), npc.getCampaign()));
            }
            if (switching) {
                requireField(npc.getSourceMonster(), "sourceMonsterId is required for a monster-based NPC");
            }
        }
        // targetType == null => legacy free-form NPC, nothing to update.
    }

    private com.dnd.app.domain.content.Species resolveRace(UUID raceId) {
        return speciesRepository.findById(raceId)
                .orElseThrow(() -> new ResourceNotFoundException("Species not found"));
    }

    private ContentCharacterClass resolveClass(UUID classId) {
        return classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
    }

    private Set<Spell> resolveSpells(List<UUID> spellIds) {
        if (spellIds == null || spellIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<UUID> distinct = new LinkedHashSet<>(spellIds);
        List<Spell> found = spellRepository.findByIdIn(distinct);
        if (found.size() != distinct.size()) {
            throw new ResourceNotFoundException("One or more spells were not found");
        }
        return new LinkedHashSet<>(found);
    }

    // Resolves a monster usable as an NPC source within this campaign. Allowed:
    // SYSTEM monsters, this campaign's own monsters, or monsters from a homebrew
    // package attached to this campaign. Monsters from other campaigns or from
    // unattached homebrew packages are rejected.
    private Monster resolveCampaignMonster(UUID monsterId, Campaign campaign) {
        Monster monster = monsterRepository.findById(monsterId)
                .orElseThrow(() -> new ResourceNotFoundException("Monster not found"));

        boolean isSystem = monster.getCampaign() == null && monster.getHomebrew() == null;
        if (isSystem) {
            return monster;
        }
        if (monster.getCampaign() != null) {
            if (!monster.getCampaign().getId().equals(campaign.getId())) {
                throw new BadRequestException("Monster belongs to a different campaign");
            }
            return monster;
        }
        Set<UUID> packageIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaign.getId());
        if (!packageIds.contains(monster.getHomebrew().getId())) {
            throw new BadRequestException("Monster's homebrew is not available in this campaign");
        }
        return monster;
    }

    private <T> T requireField(T value, String message) {
        if (value == null) {
            throw new BadRequestException(message);
        }
        return value;
    }

    private NoteResponse toNoteResponse(NpcNote note) {
        return NoteResponse.builder()
                .id(note.getId())
                .authorId(note.getAuthor().getId())
                .authorUsername(note.getAuthor().getUsername())
                .content(note.getContent())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}
