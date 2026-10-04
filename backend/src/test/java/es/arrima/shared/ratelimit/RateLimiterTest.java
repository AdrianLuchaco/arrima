package es.arrima.shared.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.arrima.MutableClock;
import es.arrima.shared.error.ApiException;
import es.arrima.shared.error.ErrorCode;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    private final MutableClock clock = new MutableClock();
    private final RateLimiter rateLimiter = new RateLimiter(clock);

    @Test
    void allowsUpToTheLimitAndRejectsTheNextRequest() {
        RateLimitPolicy policy = RateLimitPolicy.LOGIN_PER_EMAIL;
        for (int i = 0; i < policy.limit(); i++) {
            rateLimiter.consume(policy, "paqui@example.com");
        }

        assertThatThrownBy(() -> rateLimiter.consume(policy, "paqui@example.com"))
                .isInstanceOfSatisfying(ApiException.class, e -> {
                    assertThat(e.code()).isEqualTo(ErrorCode.RATE_LIMITED);
                    assertThat((Long) e.details().get("retryAfterSeconds")).isPositive();
                });
    }

    @Test
    void countsEachSubjectSeparately() {
        RateLimitPolicy policy = RateLimitPolicy.LOGIN_PER_EMAIL;
        for (int i = 0; i < policy.limit(); i++) {
            rateLimiter.consume(policy, "paqui@example.com");
        }

        assertThatCode(() -> rateLimiter.consume(policy, "manuel@example.com")).doesNotThrowAnyException();
    }

    @Test
    void startsAgainInTheNextWindow() {
        RateLimitPolicy policy = RateLimitPolicy.LOGIN_PER_EMAIL;
        for (int i = 0; i <= policy.limit(); i++) {
            try {
                rateLimiter.consume(policy, "paqui@example.com");
            } catch (ApiException expected) {
                // the last one goes over the limit
            }
        }

        clock.advance(policy.window().plus(Duration.ofSeconds(1)));

        assertThatCode(() -> rateLimiter.consume(policy, "paqui@example.com")).doesNotThrowAnyException();
    }

    @Test
    void onceTheLimitIsUsedUpEnsureNotExceededRejects() {
        RateLimitPolicy policy = RateLimitPolicy.PUBLIC_CODE_MISS_PER_IP;
        for (int i = 0; i < policy.limit(); i++) {
            rateLimiter.consume(policy, "1.2.3.4");
        }

        assertThatThrownBy(() -> rateLimiter.ensureNotExceeded(policy, "1.2.3.4")).isInstanceOf(ApiException.class);
        assertThatCode(() -> rateLimiter.ensureNotExceeded(policy, "5.6.7.8")).doesNotThrowAnyException();
    }

    @Test
    void ensureNotExceededDoesNotCount() {
        RateLimitPolicy policy = RateLimitPolicy.PUBLIC_CODE_MISS_PER_IP;
        for (int i = 0; i < policy.limit() * 3; i++) {
            rateLimiter.ensureNotExceeded(policy, "1.2.3.4");
        }

        assertThatCode(() -> rateLimiter.consume(policy, "1.2.3.4")).doesNotThrowAnyException();
    }
}
