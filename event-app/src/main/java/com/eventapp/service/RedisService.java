package com.eventapp.service;

import com.eventapp.dto.CategoryDto;
import com.eventapp.dto.EventDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${redis.key.events}")
    private String eventsKey;

    @Value("${redis.key.categories}")
    private String categoriesKey;

    @Value("${redis.key.ttl.seconds}")
    private long ttlSeconds;

    public RedisService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public List<EventDto> getCachedEvents() {
        try {
            String json = redisTemplate.opsForValue().get(eventsKey);
            if (json == null) {
                log.warn("Cache MISS for key: {}", eventsKey);
                return Collections.emptyList();
            }
            log.debug("Cache HIT for key: {}", eventsKey);
            return objectMapper.readValue(json, new TypeReference<List<EventDto>>() {});
        } catch (Exception e) {
            log.error("Redis read error for events: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<CategoryDto> getCachedCategories() {
        try {
            String json = redisTemplate.opsForValue().get(categoriesKey);
            if (json == null) {
                log.warn("Cache MISS for key: {}", categoriesKey);
                return Collections.emptyList();
            }
            return objectMapper.readValue(json, new TypeReference<List<CategoryDto>>() {});
        } catch (Exception e) {
            log.error("Redis read error for categories: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public void cacheEvents(List<EventDto> events) {
        try {
            String json = objectMapper.writeValueAsString(events);
            redisTemplate.opsForValue().set(eventsKey, json, ttlSeconds, TimeUnit.SECONDS);
            log.info("Cached {} events → key: {} (TTL {}s)", events.size(), eventsKey, ttlSeconds);
        } catch (Exception e) {
            log.error("Failed to cache events: {}", e.getMessage());
        }
    }

    public void cacheCategories(List<CategoryDto> categories) {
        try {
            String json = objectMapper.writeValueAsString(categories);
            redisTemplate.opsForValue().set(categoriesKey, json, ttlSeconds, TimeUnit.SECONDS);
            log.info("Cached {} categories → key: {}", categories.size(), categoriesKey);
        } catch (Exception e) {
            log.error("Failed to cache categories: {}", e.getMessage());
        }
    }

    public void invalidateAll() {
        redisTemplate.delete(eventsKey);
        redisTemplate.delete(categoriesKey);
        log.info("Redis cache cleared.");
    }

    public boolean isCachePopulated() {
        return Boolean.TRUE.equals(redisTemplate.hasKey(eventsKey));
    }

    public Long getCacheTtl() {
        return redisTemplate.getExpire(eventsKey, TimeUnit.SECONDS);
    }
}
