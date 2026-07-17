package com.dnd.app.domain.enums;

/**
 * Перечисление MediaLimitCategory задаёт категории лимитов на загружаемые изображения.
 * Каждая категория несёт собственные ограничения: максимальный размер файла в байтах и
 * максимальные размеры в пикселях. Конкретная категория выбирается по типу владельца
 * ({@link MediaOwnerType}), поэтому пороги хранятся в одном месте и переиспользуются
 * новыми типами без правки конфигурации.
 */
public enum MediaLimitCategory {

    /** Портреты и аватары: небольшие изображения (5 МБ, до 2048×2048 px). */
    AVATAR(5L * 1024 * 1024, 2048, 2048),

    /** Обложки витрины: крупные изображения (10 МБ, до 4096×4096 px). */
    COVER(10L * 1024 * 1024, 4096, 4096);

    private final long maxFileSizeBytes;
    private final int maxWidthPx;
    private final int maxHeightPx;

    /**
     * Создаёт категорию лимитов с заданными порогами.
     * @param maxFileSizeBytes максимальный размер файла в байтах
     * @param maxWidthPx максимальная ширина изображения в пикселях
     * @param maxHeightPx максимальная высота изображения в пикселях
     */
    MediaLimitCategory(long maxFileSizeBytes, int maxWidthPx, int maxHeightPx) {
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.maxWidthPx = maxWidthPx;
        this.maxHeightPx = maxHeightPx;
    }

    /** @return максимальный допустимый размер файла в байтах */
    public long maxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    /** @return максимальная допустимая ширина изображения в пикселях */
    public int maxWidthPx() {
        return maxWidthPx;
    }

    /** @return максимальная допустимая высота изображения в пикселях */
    public int maxHeightPx() {
        return maxHeightPx;
    }
}
