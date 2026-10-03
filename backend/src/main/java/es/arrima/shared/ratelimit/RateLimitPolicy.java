package es.arrima.shared.ratelimit;

import java.time.Duration;

/**
 * How many requests each subject (an IP address, an e-mail...) may make per time window.
 * The limits keyed by e-mail are the ones that really protect accounts: IP addresses can be
 * shared (mobile carriers put many phones behind one address) or forged (see ClientIp).
 */
public enum RateLimitPolicy {

    LOGIN_PER_IP(30, Duration.ofMinutes(1)),
    LOGIN_PER_EMAIL(10, Duration.ofMinutes(15)),
    REGISTER_PER_IP(10, Duration.ofHours(1)),
    REFRESH_PER_IP(120, Duration.ofMinutes(1)),
    /** Only failed look-ups of public codes count: it stops anyone from guessing codes. */
    PUBLIC_CODE_MISS_PER_IP(30, Duration.ofMinutes(1)),
    PASSWORD_RESET_PER_EMAIL(3, Duration.ofHours(1)),
    PASSWORD_RESET_PER_IP(10, Duration.ofHours(1));

    private final int limit;
    private final Duration window;

    RateLimitPolicy(int limit, Duration window) {
        this.limit = limit;
        this.window = window;
    }

    public int limit() {
        return limit;
    }

    public Duration window() {
        return window;
    }
}
