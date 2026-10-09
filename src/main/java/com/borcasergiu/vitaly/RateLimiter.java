package com.borcasergiu.vitaly;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
  Fixed-window request limiter keyed by client (IP).
  Every uncached player lookup costs several FACEIT calls and a period lookup up to ~100,
  so without this one visitor could burn the whole API quota for everyone.
 */
public class RateLimiter {

    private final int maxRequests;
    private final long windowMillis;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    private static final class Window {
        long start;
        int count;
    }

    public RateLimiter(int maxRequests, long windowMillis) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
    }

    public boolean tryAcquire(String key) {
        return tryAcquire(key, System.currentTimeMillis());
    }

    boolean tryAcquire(String key, long now) {
        if (windows.size() > 10_000) {
            windows.values().removeIf(w -> now - w.start >= windowMillis);
        }
        Window w = windows.computeIfAbsent(key, k -> new Window());
        synchronized (w) {
            if (now - w.start >= windowMillis) {
                w.start = now;
                w.count = 0;
            }
            if (w.count >= maxRequests) return false;
            w.count++;
            return true;
        }
    }
}
