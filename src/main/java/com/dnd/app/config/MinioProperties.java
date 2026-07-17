package com.dnd.app.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Класс MinioProperties хранит параметры подключения к объектному хранилищу MinIO.
 * Значения приходят напрямую из переменных окружения через relaxed-binding:
 * {@code endpoint} ← {@code MINIO_ENDPOINT}, {@code accessKey} ← {@code MINIO_ACCESS_KEY},
 * {@code secretKey} ← {@code MINIO_SECRET_KEY}. Ключи намеренно НЕ дублируются в
 * application.yml плейсхолдерами: так при отсутствии {@code MINIO_ENDPOINT} свойство
 * действительно отсутствует, условие {@link ConditionalOnProperty} не срабатывает и весь
 * media-модуль остаётся выключенным (ws-роль, локальный запуск без MinIO). Когда endpoint
 * задан, но креды пусты, валидация падает на старте — быстрый отказ при мисконфиге.
 */
@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "minio")
@ConditionalOnProperty(name = "minio.endpoint")
public class MinioProperties {

    /** URL эндпоинта MinIO (например, http://map-minio:9000). */
    @NotBlank
    private String endpoint;

    /** Ключ доступа (логин) к MinIO. */
    @NotBlank
    private String accessKey;

    /** Секретный ключ (пароль) к MinIO. */
    @NotBlank
    private String secretKey;
}
