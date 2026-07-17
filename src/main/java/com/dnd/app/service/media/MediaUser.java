package com.dnd.app.service.media;

import com.dnd.app.domain.enums.Role;

import java.util.UUID;

/**
 * Record MediaUser — компактный контекст текущего пользователя для media-модуля.
 * Несёт сразу {@code id} (для квоты, поля uploaded_by и сравнения владельца), {@code username}
 * (чтобы политики переиспользовали существующие проверки доступа вроде
 * {@code CharacterAccessGuard#require(id, username)}) и {@code role} (для ADMIN-модерации).
 * Резолвится один раз в контроллере из {@code Authentication}, чтобы политики не дёргали
 * репозиторий пользователей повторно.
 *
 * @param id идентификатор пользователя
 * @param username имя пользователя (логин)
 * @param role роль пользователя
 */
public record MediaUser(UUID id, String username, Role role) {

    /** @return true, если пользователь — администратор (может модерировать любые ассеты) */
    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
