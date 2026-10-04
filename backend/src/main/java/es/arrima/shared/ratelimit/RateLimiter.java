package es.arrima.shared.ratelimit;

import es.arrima.shared.error.ApiException;
import es.arrima.shared.error.ErrorCode;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory fixed-window rate limiter. The backend runs as a single instance, so memory is enough;
 * a restart simply resets the counters.
 */
@Component
public class RateLimiter {

    /** Bounds memory if someone floods us with distinct keys (e.g. many forged IPs). */
    private static final int MAX_TRACKED_KEYS = 50_000;

    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Counts one request for the subject and rejects it if the window's limit is exceeded. */
    public void consume(RateLimitPolicy policy, String subject) {
        long now = clock.millis();
        Window window = windows.compute(key(policy, subject),
                (key, current) -> current == null || current.hasEnded(now) ? Window.start(policy, now) : current.increment());
        evictEndedWindowsIfTooMany(now);
        if (window.count() > policy.limit()) {
            reject(window, now);
        }
    }

    /**
     * Rejects the request if the subject has already used up its limit in this window, without
     * counting the request itself (for limits that only count failures).
     */
    public void ensureNotExceeded(RateLimitPolicy policy, String subject) {
        long now = clock.millis();
        Window window = windows.get(key(policy, subject));
        if (window != null && !window.hasEnded(now) && window.count() >= policy.limit()) {
            reject(window, now);
        }
    }

    private static void reject(Window window, long now) {
        long retryAfterSeconds = Math.max(1, (window.endMillis() - now + 999) / 1000);
        throw new ApiException(ErrorCode.RATE_LIMITED, Map.of("retryAfterSeconds", retryAfterSeconds));
    }

    private void evictEndedWindowsIfTooMany(long now) {
        if (windows.size() > MAX_TRACKED_KEYS) {
            windows.values().removeIf(window -> window.hasEnded(now));
        }
    }

    private static String key(RateLimitPolicy policy, String subject) {
        return policy.name() + ':' + subject;
    }

    private record Window(long endMillis, int count) {

        static Window start(RateLimitPolicy policy, long now) {
            long length = policy.window().toMillis();
            long windowStart = now - (now % length);
            return new Window(windowStart + length, 1);
        }

        Window increment() {
            return new Window(endMillis, count + 1);
        }

        boolean hasEnded(long now) {
            return now >= endMillis;
        }
    }
}
