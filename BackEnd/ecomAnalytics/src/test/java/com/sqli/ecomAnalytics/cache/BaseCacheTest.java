package com.sqli.ecomAnalytics.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@TestPropertySource(properties = {
        "spring.cache.type=simple",
        "logging.level.org.springframework.cache=DEBUG"
})
public abstract class BaseCacheTest {
    @Autowired
    protected CacheManager cacheManager;

    protected void clearAllCaches() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    protected void assertCache(String cacheName, String key, Object value) {
        Cache cache = cacheManager.getCache(cacheName);
        assertThat(cache).isNotNull();

        Cache.ValueWrapper valueWrapper = cache.get(key);
        assertThat(valueWrapper).isNotNull();
        assertThat(valueWrapper.get()).isEqualTo(value);
    }

    protected void assertEvict(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        assertThat(cache).isNotNull();

        Cache.ValueWrapper wrapper = cache.get(key);
        assertThat(wrapper).isNull();
    }
}
