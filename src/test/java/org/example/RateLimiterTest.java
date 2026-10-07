package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    @Test
    void blocksAfterLimitAndResetsNextWindow() {
        RateLimiter limiter = new RateLimiter(2, 1000);
        assertTrue(limiter.tryAcquire("a", 0));
        assertTrue(limiter.tryAcquire("a", 10));
        assertFalse(limiter.tryAcquire("a", 20));
        assertTrue(limiter.tryAcquire("b", 20), "limits are per client");
        assertTrue(limiter.tryAcquire("a", 1000), "new window");
    }
}
