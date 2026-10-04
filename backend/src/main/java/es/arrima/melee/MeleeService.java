package es.arrima.melee;

import es.arrima.club.Club;
import es.arrima.club.ClubService;
import es.arrima.club.MeleeSettings;
import es.arrima.shared.time.ArrimaTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeleeService {

    /** In the prize phase, a melee closes by itself after this long without any activity. */
    public static final Duration IDLE_CLOSE_AFTER = Duration.ofMinutes(20);

    private final MeleeRepository meleeRepository;
    private final MeleeAccess meleeAccess;
    private final ClubService clubService;
    private final PublicCodeGenerator publicCodes;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public MeleeService(MeleeRepository meleeRepository, MeleeAccess meleeAccess, ClubService clubService,
            PublicCodeGenerator publicCodes, ApplicationEventPublisher events, Clock clock) {
        this.meleeRepository = meleeRepository;
        this.meleeAccess = meleeAccess;
        this.clubService = clubService;
        this.publicCodes = publicCodes;
        this.events = events;
        this.clock = clock;
    }

    /** "Crear melé clásica": named after today's date, with the club's settings unless overridden. */
    @Transactional
    public Melee create(long clubId, NewMelee request) {
        Club club = clubService.getClub(clubId);
        MeleeSettings defaults = club.getMeleeDefaults();
        MeleeSettings settings = new MeleeSettings(
                Objects.requireNonNullElse(request.courtCount(), defaults.courtCount()),
                Objects.requireNonNullElse(request.roundsCount(), defaults.roundsCount()),
                Objects.requireNonNullElse(request.prizeCount(), defaults.prizeCount()),
                Objects.requireNonNullElse(request.entryFeeCents(), defaults.entryFeeCents()));
        Melee melee = new Melee(clubId, ArrimaTime.today(clock), request.teamSize(), settings, club.getScoring(),
                publicCodes.newCode(), clock.instant());
        return meleeRepository.save(melee);
    }

    @Transactional(readOnly = true)
    public List<Melee> listForClub(long clubId) {
        return meleeRepository.findByClubIdOrderByPlayedOnDescIdDesc(clubId);
    }

    /**
     * Settings can change until the court schedule exists (it depends on rounds and courts); the
     * entry fee only before the draw (see Melee#changeSettings).
     */
    @Transactional
    public void changeSettings(long meleeId, long clubId, MeleeSettings settings) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        melee.requireStatus(MeleeStatus.REGISTRATION, MeleeStatus.TEAMS);
        melee.changeSettings(settings, clock.instant());
        meleeAccess.recordChange(melee);
    }

    @Transactional
    public void goBack(long meleeId, long clubId) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        melee.goBack(clock.instant());
        meleeAccess.recordChange(melee);
    }

    @Transactional
    public void close(long meleeId, long clubId) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        melee.close(clock.instant());
        meleeAccess.recordChange(melee);
    }

    /**
     * The 20-minute automatic close. Render's free instance may be asleep, so nothing runs on a timer:
     * it is checked whenever someone looks at the melee (admin or spectator) or at the list.
     */
    @Transactional
    public void closeIfIdle(long meleeId) {
        meleeRepository.findById(meleeId).ifPresent(this::closeIfIdle);
    }

    @Transactional
    public void closeIfIdle(long meleeId, long clubId) {
        closeIfIdle(meleeAccess.forClub(meleeId, clubId));
    }

    @Transactional
    public void closeIdleMelees(long clubId) {
        meleeRepository.findByClubIdOrderByPlayedOnDescIdDesc(clubId).forEach(this::closeIfIdle);
    }

    private void closeIfIdle(Melee melee) {
        Instant now = clock.instant();
        boolean idle = !melee.getLastActivityAt().plus(IDLE_CLOSE_AFTER).isAfter(now);
        if (melee.getStatus() == MeleeStatus.PRIZES && idle) {
            melee.close(now);
            meleeAccess.recordChange(melee);
        }
    }

    @Transactional
    public void delete(long meleeId, long clubId) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        events.publishEvent(new MeleeDeletionEvent(melee.getId()));
        meleeRepository.delete(melee);
    }
}
