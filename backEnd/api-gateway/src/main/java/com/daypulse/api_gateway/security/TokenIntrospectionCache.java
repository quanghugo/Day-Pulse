package com.daypulse.api_gateway.security;

import com.daypulse.api_gateway.dto.IntrospectResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory cache for token introspection results.
 * 
 * Reduces load on auth-service by caching validation results.
 * For production, consider using Redis or Caffeine cache.
 * 
 * Cache TTL: 30 seconds (configurable via SecurityConstants)
 */
@Slf4j
@Component
public class TokenIntrospectionCache {

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = SecurityConstants.INTROSPECTION_CACHE_TTL_SECONDS * 1000;

    /**
     * Get cached introspection result.
     * 
     * @param token JWT token
     * @return Cached result or null if not found/expired
     */
    public IntrospectResponse get(String token) {
        CacheEntry entry = cache.get(token);
        if (entry == null) {
            return null;
        }
        
        if (System.currentTimeMillis() - entry.timestamp > CACHE_TTL_MS) {
            cache.remove(token);
            return null;
        }
        
        return entry.response;
    }

    /**
     * Put introspection result in cache.
     * 
     * @param token JWT token
     * @param response Introspection response
     */
    public void put(String token, IntrospectResponse response) {
        cache.put(token, new CacheEntry(response, System.currentTimeMillis()));
    }

    /**
     * Remove token from cache (e.g., on logout).
     * 
     * @param token JWT token
     */
    public void evict(String token) {
        cache.remove(token);
    }

    /**
     * Clear all cached entries.
     */
    public void clear() {
        cache.clear();
    }

    private static class CacheEntry {
        final IntrospectResponse response;
        final long timestamp;

        CacheEntry(IntrospectResponse response, long timestamp) {
            this.response = response;
            this.timestamp = timestamp;
        }
    }
}
