package es.arrima.timer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CountdownTest {

    private static final Instant START = Instant.parse("2026-10-04T10:00:00Z");
    private static final Duration MATCH = Duration.ofMinutes(45);

    @Test
    void countsDownFromTheStart() {
        Countdown countdown = Countdown.start(START, MATCH);

        assertThat(countdown.remaining(START)).isEqualTo(MATCH);
        assertThat(countdown.remaining(START.plusSeconds(1))).isEqualTo(Duration.ofMinutes(44).plusSeconds(59));
        assertThat(countdown.endsAt()).isEqualTo(START.plus(MATCH));
    }

    @Test
    void isDueExactlyWhenTheTimeRunsOutAndNeverGoesBelowZero() {
        Countdown countdown = Countdown.start(START, MATCH);

        assertThat(countdown.isDue(START.plus(MATCH).minusMillis(1))).isFalse();
        assertThat(countdown.isDue(START.plus(MATCH))).isTrue();
        assertThat(countdown.remaining(START.plus(MATCH).plusSeconds(30))).isZero();
    }

    @Test
    void aPauseStopsTheClockAndMovesTheEndBack() {
        Countdown paused = Countdown.start(START, MATCH).pause(START.plus(Duration.ofMinutes(10)));

        // While paused, the time left stays at 35 minutes however long the rain lasts.
        assertThat(paused.state()).isEqualTo(Countdown.State.PAUSED);
        assertThat(paused.remaining(START.plus(Duration.ofMinutes(25)))).isEqualTo(Duration.ofMinutes(35));
        assertThat(paused.isDue(START.plus(Duration.ofHours(2)))).isFalse();

        Countdown resumed = paused.resume(START.plus(Duration.ofMinutes(25)));

        assertThat(resumed.state()).isEqualTo(Countdown.State.RUNNING);
        assertThat(resumed.endsAt()).isEqualTo(START.plus(MATCH).plus(Duration.ofMinutes(15)));
        assertThat(resumed.remaining(START.plus(Duration.ofMinutes(25)))).isEqualTo(Duration.ofMinutes(35));
    }

    @Test
    void severalPausesAddUp() {
        Countdown countdown = Countdown.start(START, MATCH)
                .pause(START.plusSeconds(60)).resume(START.plusSeconds(120))
                .pause(START.plusSeconds(600)).resume(START.plusSeconds(900));

        assertThat(countdown.pausedBefore()).isEqualTo(Duration.ofSeconds(360));
        assertThat(countdown.endsAt()).isEqualTo(START.plus(MATCH).plusSeconds(360));
    }

    @Test
    void onlyARunningCountdownWithTimeLeftCanBePausedAndOnlyAPausedOneResumed() {
        Countdown countdown = Countdown.start(START, MATCH);

        assertThatThrownBy(() -> countdown.resume(START)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> countdown.pause(START.plus(MATCH))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> countdown.end(START).pause(START)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void anEndedCountdownHasNothingLeft() {
        Countdown ended = Countdown.start(START, MATCH).end(START.plus(Duration.ofMinutes(20)));

        assertThat(ended.state()).isEqualTo(Countdown.State.ENDED);
        assertThat(ended.remaining(START)).isZero();
        assertThat(ended.isDue(START.plus(Duration.ofHours(1)))).isFalse();
    }
}
