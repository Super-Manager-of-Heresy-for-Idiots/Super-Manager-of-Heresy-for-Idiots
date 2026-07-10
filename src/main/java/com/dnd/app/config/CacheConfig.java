package com.dnd.app.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Класс CacheConfig описывает конфигурационный компонент, который подключает инфраструктуру к бизнес-сценариям приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String VANILLA_CLASSES = "vanillaClasses";
    public static final String VANILLA_BACKGROUNDS = "vanillaBackgrounds";
    public static final String VANILLA_SKILLS = "vanillaSkills";
    public static final String VANILLA_STAT_TYPES = "vanillaStatTypes";
    public static final String VANILLA_CURRENCIES = "vanillaCurrencies";
    public static final String VANILLA_SPELLS = "vanillaSpells";
    /** New content-model core classes; evicted explicitly on core class authoring. */
    public static final String CONTENT_VANILLA_CLASSES = "contentVanillaClasses";

    /**
     * Выполняет операции "cache manager" в рамках бизнес-логики инфраструктуры.
     * @return результат выполнения бизнес-операции
     */
    @Value("${app.cache.reference.ttl-minutes:60}")
    private long referenceTtlMinutes;

    @Value("${app.cache.reference.max-size:1000}")
    private long referenceMaxSize;

    /**
     * Выполняет операции "cache manager" в рамках бизнес-логики инфраструктуры.
     * @return результат выполнения бизнес-операции
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                VANILLA_CLASSES, VANILLA_BACKGROUNDS, VANILLA_SKILLS,
                VANILLA_STAT_TYPES, VANILLA_CURRENCIES, VANILLA_SPELLS, CONTENT_VANILLA_CLASSES);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(referenceTtlMinutes))
                .maximumSize(referenceMaxSize)
                .recordStats());
        return manager;
    }
}
