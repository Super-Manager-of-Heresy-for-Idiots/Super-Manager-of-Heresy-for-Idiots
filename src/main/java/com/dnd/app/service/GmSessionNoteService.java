package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.CreateGmNoteRequest;
import com.dnd.app.dto.request.UpdateGmNoteRequest;
import com.dnd.app.dto.response.GmSessionNoteResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.GmSessionNoteRepository;
import com.dnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Класс GmSessionNoteService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GmSessionNoteService {

    private final GmSessionNoteRepository noteRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;

    /**
     * Создает результат операции "create note" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public GmSessionNoteResponse createNote(UUID campaignId, CreateGmNoteRequest request, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);

        GmSessionNote note = GmSessionNote.builder()
                .campaign(campaign)
                .author(user)
                .title(request.getTitle())
                .content(request.getContent())
                .build();
        note = noteRepository.save(note);

        log.info("GM session note created: id={}, campaignId={}, by={}", note.getId(), campaignId, username);
        return toResponse(note);
    }

    /**
     * Возвращает список для операции "list notes" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<GmSessionNoteResponse> listNotes(UUID campaignId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);

        return noteRepository.findByCampaignId(campaignId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Возвращает результат операции "get note" в рамках бизнес-логики домена.
     * @param noteId идентификатор note, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public GmSessionNoteResponse getNote(UUID noteId, String username) {
        User user = getUser(username);
        GmSessionNote note = findNote(noteId);
        campaignService.enforceGmOrAdmin(note.getCampaign(), user);

        return toResponse(note);
    }

    /**
     * Обновляет результат операции "update note" в рамках бизнес-логики домена.
     * @param noteId идентификатор note, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public GmSessionNoteResponse updateNote(UUID noteId, UpdateGmNoteRequest request, String username) {
        User user = getUser(username);
        GmSessionNote note = findNote(noteId);
        campaignService.enforceGmOrAdmin(note.getCampaign(), user);

        if (!note.getAuthor().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only the note author or an admin can update it");
        }

        if (request.getTitle() != null) note.setTitle(request.getTitle());
        if (request.getContent() != null) note.setContent(request.getContent());
        note = noteRepository.save(note);

        log.info("GM session note updated: id={}, by={}", noteId, username);
        return toResponse(note);
    }

    /**
     * Удаляет результат операции "delete note" в рамках бизнес-логики домена.
     * @param noteId идентификатор note, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void deleteNote(UUID noteId, String username) {
        User user = getUser(username);
        GmSessionNote note = findNote(noteId);
        campaignService.enforceGmOrAdmin(note.getCampaign(), user);

        noteRepository.delete(note);
        log.info("GM session note deleted: id={}, by={}", noteId, username);
    }

    // --- Private helpers ---

    private GmSessionNote findNote(UUID noteId) {
        return noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("GM session note not found"));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private GmSessionNoteResponse toResponse(GmSessionNote note) {
        return GmSessionNoteResponse.builder()
                .id(note.getId())
                .campaignId(note.getCampaign().getId())
                .authorUsername(note.getAuthor().getUsername())
                .title(note.getTitle())
                .content(note.getContent())
                .createdAt(note.getCreatedAt())
                .updatedAt(note.getUpdatedAt())
                .build();
    }
}
