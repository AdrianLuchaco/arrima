package es.arrima.melee;

import es.arrima.club.Club;
import es.arrima.club.ClubService;
import es.arrima.club.MeleeSettings;
import es.arrima.shared.time.ArrimaTime;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeleeService {

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
                Objects.requireNonNullElse(request.prizeCount(), defaults.prizeCount()));
        Melee melee = new Melee(clubId, ArrimaTime.today(clock), request.teamSize(), settings, club.getScoring(),
                publicCodes.newCode(), clock.instant());
        return meleeRepository.save(melee);
    }

    @Transactional(readOnly = true)
    public List<Melee> listForClub(long clubId) {
        return meleeRepository.findByClubIdOrderByPlayedOnDescIdDesc(clubId);
    }

    /** Settings can change until the court schedule exists (it depends on rounds and courts). */
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

    @Transactional
    public void delete(long meleeId, long clubId) {
        Melee melee = meleeAccess.forClub(meleeId, clubId);
        events.publishEvent(new MeleeDeletionEvent(melee.getId()));
        meleeRepository.delete(melee);
    }
}
