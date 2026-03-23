package com.purpleclay.jewelry.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class CacheMetrics {

    private final RedisTemplate<String, Object> redisTemplate;

    // In-memory counters per cache name
    private final Map<String, AtomicLong> hitCounters  = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> missCounters = new ConcurrentHashMap<>();

    public CacheMetrics(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void recordHit(String cacheName) {
        hitCounters.computeIfAbsent(cacheName, k -> new AtomicLong()).incrementAndGet();
    }

    public void recordMiss(String cacheName) {
        missCounters.computeIfAbsent(cacheName, k -> new AtomicLong()).incrementAndGet();
    }

    public double getHitRate(String cacheName) {
        long hits   = hitCounters.getOrDefault(cacheName, new AtomicLong()).get();
        long misses = missCounters.getOrDefault(cacheName, new AtomicLong()).get();
        long total  = hits + misses;
        return total == 0 ? 0.0 : (double) hits / total * 100;
    }

    /** Logs cache performance every 5 minutes */
    @Scheduled(fixedRate = 300_000)
    public void logCacheStats() {
        for (String cache : hitCounters.keySet()) {
            long hits   = hitCounters.getOrDefault(cache, new AtomicLong()).get();
            long misses = missCounters.getOrDefault(cache, new AtomicLong()).get();
            double rate = getHitRate(cache);
            log.info("CACHE_STATS cache={} hits={} misses={} hitRate={:.1f}%", cache, hits, misses, rate);
        }

        // Log Redis memory info
        try {
            Object info = redisTemplate.getConnectionFactory()
                .getConnection().serverCommands().info("memory");
            if (info != null) {
                String infoStr = info.toString();
                String usedMemory = extractRedisField(infoStr, "used_memory_human");
                log.info("REDIS_MEMORY used={}", usedMemory);
            }
        } catch (Exception e) {
            log.warn("Could not fetch Redis memory stats: {}", e.getMessage());
        }
    }

    private String extractRedisField(String info, String field) {
        for (String line : info.split("\r?\n")) {
            if (line.startsWith(field + ":")) {
                return line.split(":")[1].trim();
            }
        }
        return "unknown";
    }
}
