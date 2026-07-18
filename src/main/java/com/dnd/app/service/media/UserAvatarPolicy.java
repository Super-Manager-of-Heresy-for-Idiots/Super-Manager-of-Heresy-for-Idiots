package com.dnd.app.service.media;

import com.dnd.app.domain.enums.MediaOwnerType;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Класс UserAvatarPolicy — политика прав на аватар пользователя ({@code USER_AVATAR}).
 * Симметричная и простая: менять аватар может только сам пользователь (или ADMIN); читать — любой
 * аутентифицированный (аватары видны в AccountSwitcher, списке друзей, участниках кампании).
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "minio.endpoint")
public class UserAvatarPolicy implements MediaOwnerPolicy {

    private final UserRepository userRepository;

    /** @return тип владельца — аватар пользователя */
    @Override
    public MediaOwnerType type() {
        return MediaOwnerType.USER_AVATAR;
    }

    /**
     * Разрешает менять аватар только самому пользователю или ADMIN.
     * @param ownerId идентификатор пользователя-владельца
     * @param user текущий пользователь
     */
    @Override
    public void checkUpload(UUID ownerId, MediaUser user) {
        if (!user.isAdmin() && !user.id().equals(ownerId)) {
            throw new AccessDeniedException("Аватар может менять только сам пользователь.");
        }
    }

    /**
     * Разрешает чтение аватара любому аутентифицированному пользователю.
     * @param ownerId идентификатор пользователя-владельца
     * @param user текущий пользователь
     */
    @Override
    public void checkRead(UUID ownerId, MediaUser user) {
        // Аватар пользователя виден любому аутентифицированному — дополнительных проверок нет.
    }

    /**
     * Проверяет существование пользователя-владельца до загрузки.
     * @param ownerId идентификатор пользователя
     */
    @Override
    public void validateOwnerExists(UUID ownerId) {
        if (!userRepository.existsById(ownerId)) {
            throw new ResourceNotFoundException("Пользователь не найден.");
        }
    }
}
