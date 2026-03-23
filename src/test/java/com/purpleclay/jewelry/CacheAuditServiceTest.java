package com.purpleclay.jewelry;

import com.purpleclay.jewelry.service.CacheAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheAuditServiceTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks private CacheAuditService cacheAuditService;

    @Test
    void auditAll_noKeys_returnsEmptyReport() {
        when(redisTemplate.keys(anyString())).thenReturn(Set.of());

        CacheAuditService.CacheHealthReport report = cacheAuditService.auditAll();

        assertNotNull(report);
        assertEquals(0, report.totalKeys());
        assertEquals(0, report.healthyKeys());
    }

    @Test
    void auditAll_keysWithValidTTL_markedOK() {
        when(redisTemplate.keys("products::*")).thenReturn(Set.of("products::1", "products::2"));
        when(redisTemplate.keys(argThat(k -> !k.startsWith("products")))).thenReturn(Set.of());
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(500L);

        CacheAuditService.CacheHealthReport report = cacheAuditService.auditAll();

        assertEquals(2, report.totalKeys());
        report.entries().forEach(e -> assertEquals("OK", e.status()));
    }

    @Test
    void auditAll_keyWithNoTTL_markedNoTTL() {
        when(redisTemplate.keys("products::*")).thenReturn(Set.of("products::orphan"));
        when(redisTemplate.keys(argThat(k -> !k.startsWith("products")))).thenReturn(Set.of());
        when(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).thenReturn(-1L);

        CacheAuditService.CacheHealthReport report = cacheAuditService.auditAll();

        assertEquals(1, report.noTTLKeys());
    }

    @Test
    void evictCache_deletesMatchingKeys() {
        when(redisTemplate.keys("products::*")).thenReturn(Set.of("products::1", "products::2"));
        when(redisTemplate.delete(anyCollection())).thenReturn(2L);

        int deleted = cacheAuditService.evictCache("products");

        assertEquals(2, deleted);
        verify(redisTemplate).delete(anyCollection());
    }

    @Test
    void evictCache_noKeys_returnsZero() {
        when(redisTemplate.keys("products::*")).thenReturn(Set.of());

        int deleted = cacheAuditService.evictCache("products");

        assertEquals(0, deleted);
        verify(redisTemplate, never()).delete(anyCollection());
    }
}
