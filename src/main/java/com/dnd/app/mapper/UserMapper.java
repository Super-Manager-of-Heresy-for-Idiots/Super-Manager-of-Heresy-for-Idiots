package com.dnd.app.mapper;

import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.MediaOwnerType;
import com.dnd.app.dto.response.UserResponse;
import com.dnd.app.service.media.MediaUrlResolver;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Контракт UserMapper описывает маппер, который преобразует доменные модели и DTO без изменения бизнес-правил.
 * Реализован абстрактным классом (а не интерфейсом), чтобы через {@link AfterMapping} дозаполнять
 * {@code avatarUrl} из media-модуля единой точкой для всех вызывающих (me/login/register/админ-список).
 */
@Mapper(componentModel = "spring")
public abstract class UserMapper {

    @Autowired
    protected MediaUrlResolver mediaUrlResolver;

    /**
     * Преобразует пользователя в DTO ответа.
     * @param user доменный пользователь
     * @return DTO ответа
     */
    @Mapping(target = "role", expression = "java(user.getRole().name())")
    public abstract UserResponse toResponse(User user);

    /**
     * Дозаполняет URL аватара пользователя после базового маппинга: media-ассет или null.
     * @param response собираемый DTO ответа
     * @param user исходный доменный пользователь
     */
    @AfterMapping
    protected void addAvatarUrl(@MappingTarget UserResponse response, User user) {
        response.setAvatarUrl(mediaUrlResolver.resolve(MediaOwnerType.USER_AVATAR, user.getId(), null));
    }
}
