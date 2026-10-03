package com.himanshu.social_post_backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration class enabling Spring's declarative caching abstraction
 * and configuring in-memory cache stores for optimized read operations.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    public static final String CACHE_POSTS = "posts";
    public static final String CACHE_POST_SLUGS = "post-slugs";
    public static final String CACHE_AUTHOR_STATS = "author-stats";

    @Bean
    public CacheManager cacheManager() {
        log.info("Initializing in-memory ConcurrentMapCacheManager with caches: [{}, {}, {}]",
                CACHE_POSTS, CACHE_POST_SLUGS, CACHE_AUTHOR_STATS);
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(List.of(CACHE_POSTS, CACHE_POST_SLUGS, CACHE_AUTHOR_STATS));
        return cacheManager;
    }
}
