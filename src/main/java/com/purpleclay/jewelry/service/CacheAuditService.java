package com.purpleclay.jewelry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheAuditService {

    private final RedisTemplate<String, Object> redisTemplate;

    /** Cache names and their expected max TTLs in seconds */
    private static final Map<String, Long> EXPECTED_TTL = Map.of(
        "products",         900L,   // 15 min
        "recommendations",  300L,   // 5 min
        "categories",       3600L,  // 1 hour
        "chatContext",      600L,   // 10 min
        "customization",    21600L  // 6 hours
    );

    /**
     * Full cache health report — call manually or on schedule.
     */
    public CacheHealthReport auditAll() {
        List<CacheEntry> entries = new ArrayList<>();
        long totalKeys = 0;

        for (Map.Entry<String, Long> config : EXPECTED_TTL.entrySet()) {
            String prefix = config.getKey() + "::";
            Set<String> keys = redisTemplate.keys(prefix + "*");

            if (keys == null) keys = Collections.emptySet();
            totalKeys += keys.size();

            for (String key : keys) {
                Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                String status = evaluateTTL(ttl, config.getValue());
                entries.add(new CacheEntry(key, ttl, status));
            }

            log.debug("CACHE_AUDIT cache={} keyCount={}", config.getKey(), keys.size());
        }

        long healthy  = entries.stream().filter(e -> "OK".equals(e.status())).count();
        long expiring = entries.stream().filter(e -> "EXPIRING_SOON".equals(e.status())).count();
        long orphaned = entries.stream().filter(e -> "NO_TTL".equals(e.status())).count();

        CacheHealthReport report = new CacheHealthReport(totalKeys, healthy, expiring, orphaned, entries);
        log.info("CACHE_AUDIT_COMPLETE total={} healthy={} expiringSoon={} noTTL={}",
            totalKeys, healthy, expiring, orphaned);
        return report;
    }

    /**
     * Force evict all keys for a specific cache.
     */
    public int evictCache(String cacheName) {
        String pattern = cacheName + "::*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys == null || keys.isEmpty()) return 0;
        redisTemplate.delete(keys);
        log.info("CACHE_EVICT cache={} keysDeleted={}", cacheName, keys.size());
        return keys.size();
    }

    /**
     * Evict all Redis keys (full flush — use with caution).
     */
    public void evictAll() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        log.warn("CACHE_FLUSH_ALL - entire Redis DB flushed by admin");
    }

    /** Runs every hour to log cache state */
    @Scheduled(fixedRate = 3_600_000)
    public void scheduledAudit() {
        auditAll();
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private String evaluateTTL(Long ttl, Long expectedMax) {
        if (ttl == null || ttl == -1) return "NO_TTL";      // No expiry set — potential memory leak
        if (ttl < 60)                 return "EXPIRING_SOON";
        if (ttl > expectedMax)        return "TTL_EXCEEDED"; // Shouldn't happen, but flag it
        return "OK";
    }

    // ─── Response types ───────────────────────────────────────────────────────

    public record CacheEntry(String key, Long ttlSeconds, String status) {}

    public record CacheHealthReport(
        long totalKeys,
        long healthyKeys,
        long expiringSoon,
        long noTTLKeys,
        List<CacheEntry> entries
    ) {}
}
