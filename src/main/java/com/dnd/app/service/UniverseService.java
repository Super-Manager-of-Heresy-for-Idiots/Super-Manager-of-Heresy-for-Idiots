package com.dnd.app.service;

import com.dnd.app.domain.Universe;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.CreateUniverseRequest;
import com.dnd.app.dto.response.UniverseResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.UniverseRepository;
import com.dnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Класс UniverseService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UniverseService {

    private final UniverseRepository universeRepository;
    private final UserRepository userRepository;

    /**
     * Возвращает список для операции "list universes" в рамках бизнес-логики домена.
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<UniverseResponse> listUniverses(String username) {
        // Any authenticated user (incl. PLAYER) may read universes for selection.
        getUser(username);
        return universeRepository.findAllByOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Создает результат операции "create universe" в рамках бизнес-логики домена.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public UniverseResponse createUniverse(CreateUniverseRequest request, String username) {
        User user = getAuthoringUser(username);

        String slug = request.getSlug().toLowerCase();
        if (universeRepository.existsBySlug(slug)) {
            throw new DuplicateResourceException("Вселенная с таким слагом уже существует");
        }

        Universe universe = Universe.builder()
                .slug(slug)
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(user)
                .isSystem(false)
                .build();
        universe = universeRepository.save(universe);

        log.info("Universe created: id={}, slug='{}', by={}", universe.getId(), universe.getSlug(), username);
        return toResponse(universe);
    }

    private UniverseResponse toResponse(Universe universe) {
        return UniverseResponse.builder()
                .id(universe.getId())
                .slug(universe.getSlug())
                .name(universe.getName())
                .description(universe.getDescription())
                .isSystem(universe.getIsSystem())
                .createdAt(universe.getCreatedAt())
                .build();
    }

    private User getAuthoringUser(String username) {
        User user = getUser(username);
        if (user.getRole() != Role.GAME_MASTER && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Только мастера игры могут создавать вселенные");
        }
        return user;
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }
}
