package com.dnd.app.config;

import com.dnd.app.domain.enums.MediaLimitCategory;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Класс MediaProperties хранит параметры media-модуля, не зависящие от наличия MinIO,
 * поэтому существует всегда (в отличие от {@link MinioProperties}). {@code bucket} —
 * имя бакета для ассетов core ({@code app.media.bucket}, по умолчанию {@code dnd-core-assets});
 * {@code userQuotaBytes} — суммарная квота размера ассетов на одного пользователя
 * (по умолчанию 100 МБ). Пер-категорийные лимиты размера/пикселей живут в
 * {@link MediaLimitCategory}, потому что различаются по значениям и не требуют внешней настройки.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.media")
public class MediaProperties {

    /** Имя бакета MinIO, в котором хранятся ассеты core. */
    private String bucket = "dnd-core-assets";

    /** Квота суммарного размера ассетов на одного пользователя, в байтах (по умолчанию 100 МБ). */
    private long userQuotaBytes = 100L * 1024 * 1024;
}
