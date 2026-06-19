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
 * Caching for read-mostly, effectively-static system ("vanilla") 5e reference data.
 *
 * <p>Deliberately a <b>local per-pod</b> Caffeine cache rather than a distributed one:
 * <ul>
 *   <li>The cached data ({@link com.dnd.app.service.ReferenceDataService} vanilla
 *       classes/races/backgrounds/skills/stat-types/currencies/spells) is system-seeded
 *       reference data that changes only via migrations/deploys, so cross-pod staleness
 *       is bounded by a short TTL and needs no shared invalidation channel.</li>
 *   <li>A local cache adds zero infrastructure (no Redis), survives pod restarts cleanly,
 *       and each pod warms independently — which scales linearly as pods are added.</li>
 * </ul>
 *
 * <p>If campaign-scoped or homebrew-affected reference data is cached later, that data
 * <em>does</em> mutate at runtime and would require either a very short TTL or a distributed
 * cache with explicit eviction (Redis). Keep mutable data out of this cache manager.
 *
 * <p>TTL and size are env-configurable so memory footprint can be tuned per pod resource grant.
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

    @Value("${app.cache.reference.ttl-minutes:60}")
    private long referenceTtlMinutes;

    @Value("${app.cache.reference.max-size:1000}")
    private long referenceMaxSize;

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
