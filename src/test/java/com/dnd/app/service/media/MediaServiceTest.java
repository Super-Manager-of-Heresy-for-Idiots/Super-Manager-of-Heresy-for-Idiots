package com.dnd.app.service.media;

import com.dnd.app.config.MediaProperties;
import com.dnd.app.domain.MediaAsset;
import com.dnd.app.domain.enums.MediaOwnerType;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.response.MediaAssetResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.MediaAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaService: загрузка, замена слота, квота и валидация файла")
class MediaServiceTest {

    @Mock
    private MediaAssetRepository repository;
    @Mock
    private AssetStorage storage;
    @Mock
    private MediaPolicyRegistry policyRegistry;
    @Mock
    private MediaOwnerPolicy policy;

    // Storage keys и properties чистые/дефолтные — используем реальные экземпляры, не моки.
    private final MediaStorageKeys storageKeys = new MediaStorageKeys();
    private final MediaProperties properties = new MediaProperties();

    private MediaService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();
    private final MediaUser user = new MediaUser(userId, "alice", Role.PLAYER);

    @BeforeEach
    void setUp() {
        service = new MediaService(repository, storage, storageKeys, properties, policyRegistry);
    }

    @Test
    @DisplayName("Замена: старый ассет и его объект удаляются, новый сохраняется")
    void upload_replacesExistingSlot() throws IOException {
        when(policyRegistry.require(MediaOwnerType.CHARACTER_AVATAR)).thenReturn(policy);
        MediaAsset old = MediaAsset.builder()
                .id(UUID.randomUUID())
                .ownerType(MediaOwnerType.CHARACTER_AVATAR)
                .ownerId(ownerId)
                .bucketName("dnd-core-assets")
                .storageKey("media/character_avatar/old/key.png")
                .sizeBytes(1000)
                .uploadedBy(userId)
                .build();
        when(repository.findByOwnerTypeAndOwnerId(MediaOwnerType.CHARACTER_AVATAR, ownerId))
                .thenReturn(Optional.of(old));
        when(repository.sumSizeBytesByUploadedBy(userId)).thenReturn(1000L);
        when(repository.save(any(MediaAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", pngBytes());

        MediaAssetResponse response = service.upload(MediaOwnerType.CHARACTER_AVATAR, ownerId, file, user);

        verify(storage).save(eq("dnd-core-assets"), anyString(), eq("image/png"), anyLong(), any());
        verify(repository).delete(old);
        // Без активной транзакции в юнит-тесте удаление старого объекта выполняется синхронно.
        verify(storage).delete("dnd-core-assets", "media/character_avatar/old/key.png");
        assertEquals("image/png", response.contentType());
        assertTrue(response.url().startsWith("/api/media/"));
        assertTrue(response.url().endsWith("/content"));
    }

    @Test
    @DisplayName("Превышение квоты пользователя → 400, файл в хранилище не заливается")
    void upload_overQuota_throwsAndSkipsStorage() throws IOException {
        when(policyRegistry.require(MediaOwnerType.CHARACTER_AVATAR)).thenReturn(policy);
        when(repository.findByOwnerTypeAndOwnerId(MediaOwnerType.CHARACTER_AVATAR, ownerId))
                .thenReturn(Optional.empty());
        when(repository.sumSizeBytesByUploadedBy(userId)).thenReturn(properties.getUserQuotaBytes());

        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", pngBytes());

        assertThrows(BadRequestException.class,
                () -> service.upload(MediaOwnerType.CHARACTER_AVATAR, ownerId, file, user));
        verify(storage, never()).save(anyString(), anyString(), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("Отказ политики прав пробрасывается, файл не заливается")
    void upload_policyDenies_throwsAndSkipsStorage() throws IOException {
        when(policyRegistry.require(MediaOwnerType.CHARACTER_AVATAR)).thenReturn(policy);
        doThrow(new AccessDeniedException("нет прав")).when(policy).checkUpload(eq(ownerId), any());

        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", pngBytes());

        assertThrows(AccessDeniedException.class,
                () -> service.upload(MediaOwnerType.CHARACTER_AVATAR, ownerId, file, user));
        verify(storage, never()).save(anyString(), anyString(), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("Мусор под видом картинки (не декодируется) → 400")
    void upload_undecodableImage_throws() {
        when(policyRegistry.require(MediaOwnerType.CHARACTER_AVATAR)).thenReturn(policy);
        MockMultipartFile garbage = new MockMultipartFile(
                "file", "a.png", "image/png", new byte[]{1, 2, 3, 4, 5});

        assertThrows(BadRequestException.class,
                () -> service.upload(MediaOwnerType.CHARACTER_AVATAR, ownerId, garbage, user));
        verify(storage, never()).save(anyString(), anyString(), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("Неразрешённый content-type → 400")
    void upload_disallowedContentType_throws() {
        when(policyRegistry.require(MediaOwnerType.CHARACTER_AVATAR)).thenReturn(policy);
        MockMultipartFile textFile = new MockMultipartFile(
                "file", "a.txt", "text/plain", "hello".getBytes());

        assertThrows(BadRequestException.class,
                () -> service.upload(MediaOwnerType.CHARACTER_AVATAR, ownerId, textFile, user));
        verify(storage, never()).save(anyString(), anyString(), anyString(), anyLong(), any());
    }

    /**
     * Генерирует байты валидного PNG-изображения 10×10 для позитивных сценариев загрузки.
     * @return байты PNG
     * @throws IOException при ошибке кодирования
     */
    private static byte[] pngBytes() throws IOException {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
