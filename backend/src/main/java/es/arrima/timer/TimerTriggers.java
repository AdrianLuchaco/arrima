package es.arrima.timer;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * What notices that a countdown ran out, so the notifications go out on time. Recording the end is
 * idempotent (TimerService), so the triggers overlap without harm:
 * <ol>
 *   <li>a task scheduled in memory for the exact moment, when a countdown starts or resumes;</li>
 *   <li>at start-up, the same for the countdowns still running (memory is lost on a restart);</li>
 *   <li>every 30 seconds, a check of all of them, in case anything above was missed;</li>
 *   <li>outside this class: any look at the melee, and the admin's phone when its countdown reaches 0.</li>
 * </ol>
 */
@Component
class TimerTriggers {

    private static final Logger log = LoggerFactory.getLogger(TimerTriggers.class);
    /** Checked a moment after the end, so the countdown is surely due. */
    private static final Duration MARGIN = Duration.ofMillis(200);

    private final TimerService timerService;
    private final TaskScheduler scheduler;

    TimerTriggers(TimerService timerService, TaskScheduler scheduler) {
        this.timerService = timerService;
        this.scheduler = scheduler;
    }

    /** After the commit: before it, the countdown is not in the database yet. */
    @TransactionalEventListener
    void onCountdownRunning(RunningCountdown countdown) {
        scheduleCheck(countdown.meleeId(), countdown.endsAt());
    }

    @EventListener(ApplicationReadyEvent.class)
    void recoverAfterRestart() {
        timerService.expireAllDue();
        timerService.running().forEach(countdown -> scheduleCheck(countdown.meleeId(), countdown.endsAt()));
    }

    @Scheduled(initialDelay = 30_000, fixedDelay = 30_000)
    void safetyNet() {
        try {
            timerService.expireAllDue();
        } catch (RuntimeException e) {
            log.warn("Could not check the match countdowns: {}", e.getMessage());
        }
    }

    private void scheduleCheck(long meleeId, Instant endsAt) {
        scheduler.schedule(() -> check(meleeId), endsAt.plus(MARGIN));
    }

    private void check(long meleeId) {
        try {
            timerService.expireIfDue(meleeId);
        } catch (RuntimeException e) {
            log.warn("Could not record the end of the countdown of melee {}: {}", meleeId, e.getMessage());
        }
    }
}
