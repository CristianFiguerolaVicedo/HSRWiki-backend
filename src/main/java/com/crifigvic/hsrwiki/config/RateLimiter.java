package com.crifigvic.hsrwiki.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * At most {@code limit} uses per key in each time window (fixed windows, kept in memory).
 * The table of keys is bounded too: when it reaches {@code maxKeys} the finished windows are dropped,
 * and if it is still full it starts over, so a flood of different keys can't grow memory.
 */
public class RateLimiter {

    private final int limit;
    private final long windowMillis;
    private final int maxKeys;
    private final Map<String, long[]> windows = new HashMap<>(); // key -> {window start, uses}

    public RateLimiter(int limit, Duration window, int maxKeys) {
        this.limit = limit;
        this.windowMillis = window.toMillis();
        this.maxKeys = maxKeys;
    }

    /** true if the key may go ahead now (and counts this use); false if it's over the limit. */
    public synchronized boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        if (windows.size() >= maxKeys && !windows.containsKey(key)) {
            windows.values().removeIf(w -> now - w[0] >= windowMillis);
            if (windows.size() >= maxKeys) {
                windows.clear();
            }
        }
        long[] w = windows.computeIfAbsent(key, k -> new long[]{now, 0});
        if (now - w[0] >= windowMillis) {
            w[0] = now;
            w[1] = 0;
        }
        if (w[1] >= limit) {
            return false;
        }
        w[1]++;
        return true;
    }
}
