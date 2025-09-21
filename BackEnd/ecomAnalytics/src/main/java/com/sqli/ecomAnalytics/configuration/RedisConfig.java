package com.sqli.ecomAnalytics.configuration;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

//    @Bean
//    public RedisConnectionFactory redisConnectionFactory() {
//        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
//        redisConfig.setHostName("localhost");
//        redisConfig.setPort(6379);
//        return new LettuceConnectionFactory(redisConfig);
//    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put("customer", cacheConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigurations.put("analytics", cacheConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("products", cacheConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("recommendations", cacheConfig.entryTtl(Duration.ofHours(12)));
        cacheConfigurations.put("allCustomersCache",cacheConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("customerProfileCache", cacheConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigurations.put("customerCodeCache", cacheConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigurations.put("highestPayingCustomersCache", cacheConfig.entryTtl(Duration.ofHours(6)));
        cacheConfigurations.put("productCatalogCache", cacheConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("productByIdCache", cacheConfig.entryTtl(Duration.ofHours(24)));
        cacheConfigurations.put("productByCategoryCache", cacheConfig.entryTtl(Duration.ofHours(6)));
        cacheConfigurations.put("productByBrandCache", cacheConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("productSearchCache", cacheConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("customerSpentCache", cacheConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("customersAnalyticsCache", cacheConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("kpiCache", cacheConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("productsPerformanceCache", cacheConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("salesTrendCache", cacheConfig.entryTtl(Duration.ofHours(1)));
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
