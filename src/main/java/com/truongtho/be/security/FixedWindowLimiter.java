package com.truongtho.be.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class FixedWindowLimiter {
    private final Cache<String, AtomicInteger> hits;
    private final int max;
    private final long windowSeconds;

    public FixedWindowLimiter(int max, Duration window) {
        this.max = max;
        this.windowSeconds = window.toSeconds();
        this.hits = Caffeine.newBuilder()
                .expireAfterWrite(window)
                .maximumSize(100_000)
                .build();
    }


    public boolean tryAcquire(String key) {
        return hits.get(key, k -> new AtomicInteger()).incrementAndGet() <= max;
    }

    public long windowSeconds() { return windowSeconds; }
}