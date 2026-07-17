package com.dnd.app.config;

import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Класс MinioConfig создаёт клиента MinIO для media-модуля.
 * Условие {@link ConditionalOnProperty}{@code (name = "minio.endpoint")} гарантирует, что без
 * настроенного MinIO ни клиент, ни зависящие от него бины (storage/service/controller) не
 * поднимаются — приложение стартует с полностью выключенным модулем картинок. На деплойментах,
 * где MinIO не нужен (например, ws-роль), переменную {@code MINIO_ENDPOINT} просто не задают.
 */
@Configuration
@ConditionalOnProperty(name = "minio.endpoint")
public class MinioConfig {

    /**
     * Создаёт бин клиента MinIO по параметрам подключения.
     * @param properties параметры подключения к MinIO (endpoint и учётные данные)
     * @return сконфигурированный клиент MinIO
     */
    @Bean
    MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }
}
