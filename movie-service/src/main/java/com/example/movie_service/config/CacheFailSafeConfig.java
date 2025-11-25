package com.example.movie_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CacheFailSafeConfig implements CachingConfigurer {


    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {

            // 1. Handle Read Errors (GET)
            // If Redis is down during a read, we log the error and treat it as a "Cache Miss".
            // The flow continues to the Database automatically.
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.error("Redis is down! Failed to get key [{}]. Error: {}", key, exception.getMessage());
                // We do NOT throw the exception. We just return, allowing the app to hit the DB.
            }

            // 2. Handle Write Errors (PUT)
            // If Redis is down during a write (after DB update), we log it.
            // The data is safe in the DB, just not in the cache.
            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.error("Redis is down! Failed to put key [{}]. Error: {}", key, exception.getMessage());
            }

            // 3. Handle Evict Errors (DELETE)
            // If Redis is down during a delete, we log it.
            // WARNING: This can lead to stale data if the cache comes back up and still has the old key.
            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.error("Redis is down! Failed to evict key [{}]. Error: {}", key, exception.getMessage());
            }

            // 4. Handle Clear Errors (DELETE ALL)
            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.error("Redis is down! Failed to clear cache. Error: {}", exception.getMessage());
            }
        };
    }
}
