package com.purpleclay.jewelry.controller;

import com.purpleclay.jewelry.service.CacheAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/cache")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Cache", description = "Redis cache health, audit, and eviction (Admin only)")
public class CacheAdminController {

    private final CacheAuditService cacheAuditService;

    @GetMapping("/audit")
    @Operation(summary = "Full Redis cache health report")
    public ResponseEntity<CacheAuditService.CacheHealthReport> auditCache() {
        return ResponseEntity.ok(cacheAuditService.auditAll());
    }

    @DeleteMapping("/evict/{cacheName}")
    @Operation(summary = "Evict all keys for a specific cache",
               description = "Valid cache names: products, recommendations, categories, chatContext, customization")
    public ResponseEntity<Map<String, Object>> evictCache(@PathVariable String cacheName) {
        int count = cacheAuditService.evictCache(cacheName);
        return ResponseEntity.ok(Map.of(
            "cache", cacheName,
            "keysEvicted", count,
            "message", "Cache evicted successfully"
        ));
    }

    @DeleteMapping("/flush-all")
    @Operation(summary = "Flush entire Redis DB — USE WITH CAUTION")
    public ResponseEntity<Map<String, String>> flushAll() {
        cacheAuditService.evictAll();
        return ResponseEntity.ok(Map.of("message", "All Redis keys flushed"));
    }
}
