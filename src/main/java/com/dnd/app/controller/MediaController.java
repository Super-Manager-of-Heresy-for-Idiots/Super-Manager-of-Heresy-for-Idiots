package com.dnd.app.controller;

import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.MediaOwnerType;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.MediaAssetResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.service.media.MediaService;
import com.dnd.app.service.media.MediaUser;
import com.dnd.app.service.media.StoredObjectStream;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.UUID;

/**
 * Класс MediaController — REST-контроллер media-модуля (загрузка/стрим/удаление картинок).
 * Поднимается только при заданном {@code minio.endpoint}: без MinIO эндпоинтов нет, {@code /api/media/**}
 * отвечает 404 — модуль условно выключен. Все эндпоинты доступны только аутентифицированным
 * пользователям (как остальные {@code /api/**} по SecurityConfig). {@code ownerType} в пути — строка
 * enum'а; некорректное значение конвертер отдаёт как 400.
 */
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "minio.endpoint")
@Tag(name = "Media", description = "Загрузка и выдача картинок (аватары, портреты, обложки)")
public class MediaController {

    private final MediaService mediaService;
    private final UserRepository userRepository;

    /**
     * Загружает картинку в слот владельца (multipart-поле {@code file}).
     * @param ownerType тип владельца ассета (значение enum в пути)
     * @param ownerId идентификатор владельца ассета
     * @param file загружаемый файл изображения
     * @param authentication контекст аутентификации текущего пользователя
     * @return 200 и DTO с прокси-URL на содержимое
     */
    @PostMapping(path = "/{ownerType}/{ownerId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить/заменить картинку слота владельца")
    public ResponseEntity<ApiResponse<MediaAssetResponse>> upload(
            @PathVariable MediaOwnerType ownerType,
            @PathVariable UUID ownerId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        MediaUser user = resolve(authentication);
        MediaAssetResponse response = mediaService.upload(ownerType, ownerId, file, user);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Стримит содержимое ассета с «вечным» иммутабельным кэшем (URL меняется при замене картинки).
     * @param assetId идентификатор ассета
     * @param authentication контекст аутентификации текущего пользователя
     * @return поток изображения с заголовками типа, длины и кэша
     */
    @GetMapping("/{assetId}/content")
    @Operation(summary = "Получить содержимое картинки (прокси-стрим)")
    public ResponseEntity<StreamingResponseBody> content(
            @PathVariable UUID assetId,
            Authentication authentication) {
        MediaUser user = resolve(authentication);
        StoredObjectStream object = mediaService.openContent(assetId, user);
        StreamingResponseBody body = outputStream -> {
            try (InputStream input = object.inputStream()) {
                input.transferTo(outputStream);
            }
        };
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(object.contentType()))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().build().toString());
        if (object.sizeBytes() != null) {
            builder.contentLength(object.sizeBytes());
        }
        return builder.body(body);
    }

    /**
     * Удаляет картинку слота владельца.
     * @param ownerType тип владельца ассета (значение enum в пути)
     * @param ownerId идентификатор владельца ассета
     * @param authentication контекст аутентификации текущего пользователя
     * @return 204 без тела
     */
    @DeleteMapping("/{ownerType}/{ownerId}")
    @Operation(summary = "Удалить картинку слота владельца")
    public ResponseEntity<Void> delete(
            @PathVariable MediaOwnerType ownerType,
            @PathVariable UUID ownerId,
            Authentication authentication) {
        MediaUser user = resolve(authentication);
        mediaService.delete(ownerType, ownerId, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * Резолвит текущего пользователя из {@code Authentication} в компактный {@link MediaUser}.
     * @param authentication контекст аутентификации
     * @return контекст пользователя для media-модуля
     */
    private MediaUser resolve(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AccessDeniedException("Требуется аутентификация.");
        }
        User user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден."));
        return new MediaUser(user.getId(), user.getUsername(), user.getRole());
    }
}
