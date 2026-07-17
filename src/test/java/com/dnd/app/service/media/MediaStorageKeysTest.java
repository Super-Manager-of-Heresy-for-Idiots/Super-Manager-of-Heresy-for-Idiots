package com.dnd.app.service.media;

import com.dnd.app.domain.enums.MediaOwnerType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("MediaStorageKeys: генерация ключей и санитизация имён файлов")
class MediaStorageKeysTest {

    private final MediaStorageKeys keys = new MediaStorageKeys();

    @Test
    @DisplayName("Ключ строится по схеме media/{owner_type}/{owner_id}/{asset_id}/{filename}")
    void objectKey_buildsSchemePath() {
        UUID ownerId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID assetId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        String key = keys.objectKey(MediaOwnerType.CHARACTER_AVATAR, ownerId, assetId, "My Avatar.PNG");

        assertEquals("media/character_avatar/11111111-1111-1111-1111-111111111111/"
                + "22222222-2222-2222-2222-222222222222/my-avatar.png", key);
    }

    @Test
    @DisplayName("Санитизация убирает путь (защита от traversal) и оставляет только имя файла")
    void sanitize_stripsPath() {
        assertEquals("passwd", keys.sanitizeFilename("../../etc/passwd"));
        assertEquals("evil.png", keys.sanitizeFilename("C:\\Windows\\evil.png"));
    }

    @Test
    @DisplayName("Санитизация приводит к нижнему регистру и заменяет недопустимые символы на дефис")
    void sanitize_lowercasesAndReplaces() {
        assertEquals("my-file.jpg", keys.sanitizeFilename("My File.JPG"));
    }

    @Test
    @DisplayName("Пустое/пустое-после-очистки имя заменяется на image")
    void sanitize_emptyBecomesImage() {
        assertEquals("image", keys.sanitizeFilename(null));
        assertEquals("image", keys.sanitizeFilename("   "));
    }

    @Test
    @DisplayName("Ведущие точки удаляются (нет скрытых файлов вроде .htaccess)")
    void sanitize_stripsLeadingDots() {
        assertEquals("png", keys.sanitizeFilename("....png"));
    }
}
