package es.arrima.timer;

import es.arrima.melee.Melee;
import es.arrima.melee.MeleeAccess;
import es.arrima.melee.MeleeRepository;
import es.arrima.melee.MeleeStatus;
import es.arrima.shared.error.ApiException;
import es.arrima.shared.error.ErrorCode;
import es.arrima.timer.domain.Countdown;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Empezar partida N": every match of the round starts at once and lasts the melee's match length.
 * <p>
 * Only the start, the length and the pauses are stored; every phone works out what is left (see
 * Countdown). The end is recorded exactly once whichever trigger notices it first (see
 * TimerTriggers): the alarm notifications depend on it.
 */
@Service
public class TimerService {

    private final RoundTimerRepository repository;
    private final MeleeAccess meleeAccess;
    private final MeleeRepository meleeRepository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public TimerService(RoundTimerRepository repository, MeleeAccess meleeAccess, MeleeRepository meleeRepository,
            ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.meleeAccess = meleeAccess;
        this.meleeRepository = meleeRepository;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<RoundTimer> timersOf(long meleeId) {
        return repository.findByMeleeIdOrderByRoundNumber(meleeId);
    }

    /**
     * One countdown at a time: if another round's countdown has not ended, the admin must confirm,
     * and then it just stops, without alarm (it was the previous round, already finished on the courts).
     */
    @Transactional
    public void start(long meleeId, long clubId, int roundNumber, boolean stopRunningCountdown) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        melee.requireStatus(MeleeStatus.MATCHES);
        if (roundNumber < 1 || roundNumber > melee.getSettings().roundsCount()) {
            throw ApiException.validation("roundNumber", "Range");
        }
        if (repository.findByMeleeIdAndRoundNumber(melee.getId(), roundNumber).isPresent()) {
            throw new ApiException(ErrorCode.INVALID_STATE, Map.of("reason", "ALREADY_STARTED"));
        }
        Instant now = clock.instant();
        Optional<RoundTimer> notEnded = timersOf(melee.getId()).stream()
                .filter(timer -> timer.countdown().state() != Countdown.State.ENDED)
                .findFirst();
        if (notEnded.isPresent() && !recordTimeUpIfDue(notEnded.get(), now)) {
            if (!stopRunningCountdown) {
                throw new ApiException(ErrorCode.TIMER_RUNNING, Map.of("round", notEnded.get().getRoundNumber()));
            }
            notEnded.get().stopForNextRound(now);
        }
        RoundTimer timer = repository.save(new RoundTimer(melee.getId(), roundNumber,
                Duration.ofMinutes(melee.getSettings().matchMinutes()), now));
        events.publishEvent(new RunningCountdown(melee.getId(), timer.countdown().endsAt()));
        meleeAccess.recordChange(melee);
    }

    /** Rain, an incident: the countdown stops and its end moves back as long as the pause lasts. */
    @Transactional
    public void pause(long meleeId, long clubId, int roundNumber) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        RoundTimer timer = timerOf(melee, roundNumber);
        Instant now = clock.instant();
        // Out of time already: pausing makes no sense any more, the end is recorded instead.
        if (!recordTimeUpIfDue(timer, now)) {
            requireState(timer, Countdown.State.RUNNING);
            timer.pause(now);
        }
        meleeAccess.recordChange(melee);
    }

    @Transactional
    public void resume(long meleeId, long clubId, int roundNumber) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        RoundTimer timer = timerOf(melee, roundNumber);
        requireState(timer, Countdown.State.PAUSED);
        timer.resume(clock.instant());
        events.publishEvent(new RunningCountdown(melee.getId(), timer.countdown().endsAt()));
        meleeAccess.recordChange(melee);
    }

    /** Started by mistake: as if it had never been started. Not once the time is up. */
    @Transactional
    public void cancel(long meleeId, long clubId, int roundNumber) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        RoundTimer timer = timerOf(melee, roundNumber);
        if (timer.countdown().state() == Countdown.State.ENDED) {
            throw new ApiException(ErrorCode.INVALID_STATE, Map.of("reason", "ALREADY_ENDED"));
        }
        repository.delete(timer);
        meleeAccess.recordChange(melee);
    }

    /** The countdowns belong to a court schedule: a new one starts without them. */
    @Transactional
    public void deleteAll(Melee melee) {
        repository.deleteByMeleeId(melee.getId());
    }

    /** Records the end of this melee's countdown if its time ran out. Harmless to call any number of times. */
    @Transactional
    public void expireIfDue(long meleeId) {
        Instant now = clock.instant();
        repository.findByMeleeIdOrderByRoundNumber(meleeId).forEach(timer -> recordTimeUpIfDue(timer, now));
    }

    /** The same for every melee: the safety net after a restart or a missed trigger. */
    @Transactional
    public void expireAllDue() {
        Instant now = clock.instant();
        repository.findByEndedAtIsNull().forEach(timer -> recordTimeUpIfDue(timer, now));
    }

    /** Countdowns still running, for the triggers to schedule after a restart. */
    @Transactional(readOnly = true)
    public List<RunningCountdown> running() {
        return repository.findByEndedAtIsNull().stream()
                .filter(timer -> timer.countdown().state() == Countdown.State.RUNNING)
                .map(timer -> new RunningCountdown(timer.getMeleeId(), timer.countdown().endsAt()))
                .toList();
    }

    /** @return true if the time was up (whoever recorded it), false if there is time left */
    private boolean recordTimeUpIfDue(RoundTimer timer, Instant now) {
        Countdown countdown = timer.countdown();
        if (!countdown.isDue(now)) {
            return countdown.state() == Countdown.State.ENDED;
        }
        // The end is the moment the time ran out, not when someone noticed.
        if (repository.markTimeUp(timer.getId(), timer.getPausedMillis(), countdown.endsAt()) == 1) {
            events.publishEvent(new RoundTimeUpEvent(timer.getMeleeId(), timer.getRoundNumber()));
            meleeRepository.findById(timer.getMeleeId()).ifPresent(meleeAccess::recordChange);
        }
        return true;
    }

    private RoundTimer timerOf(Melee melee, int roundNumber) {
        return repository.findByMeleeIdAndRoundNumber(melee.getId(), roundNumber).orElseThrow(ApiException::notFound);
    }

    private static void requireState(RoundTimer timer, Countdown.State expected) {
        if (timer.countdown().state() != expected) {
            throw new ApiException(ErrorCode.INVALID_STATE, Map.of("timer", timer.countdown().state().name()));
        }
    }
}
