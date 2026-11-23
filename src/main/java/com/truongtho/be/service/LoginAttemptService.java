package com.truongtho.be.service;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.truongtho.be.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class LoginAttemptService {
    private static final int MAX_FAILS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration LOCK = Duration.ofMinutes(30);

    private record Window(int count, Instant start) {}

    private final Cache<String, Window> fails = Caffeine.newBuilder()
            .expireAfterWrite(WINDOW).maximumSize(100_000).build();
    private final Cache<String, Instant> locked = Caffeine.newBuilder()
            .expireAfterWrite(LOCK).maximumSize(100_000).build();

    public void checkNotLocked(String key) {
        Instant until = locked.getIfPresent(key);
        if (until == null) return;
        long secs = Duration.between(Instant.now(), until).getSeconds();
        if (secs > 0) {
            throw new TooManyRequestsException(
                    "Bạn đã nhập sai quá " + MAX_FAILS + " lần. Vui lòng thử lại sau "
                            + ((secs + 59) / 60) + " phút.", secs);
        }
    }

    public void recordFailure(String key) {
        Instant now = Instant.now();
        Window w = fails.asMap().compute(key, (k, old) ->
                (old == null || old.start().plus(WINDOW).isBefore(now))
                        ? new Window(1, now)
                        : new Window(old.count() + 1, old.start()));
        if (w.count() >= MAX_FAILS) {
            locked.put(key, now.plus(LOCK));
            fails.invalidate(key);
        }
    }

    public void reset(String key) {
        fails.invalidate(key);
    }
}