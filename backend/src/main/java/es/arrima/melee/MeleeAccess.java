package es.arrima.melee;

import es.arrima.shared.error.ApiException;
import java.time.Clock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * The single way services load a melee for an admin, so club isolation is applied in one place:
 * a melee of another club is simply "not found" (404), never "forbidden", to avoid revealing it exists.
 */
@Component
public class MeleeAccess {

    private final MeleeRepository meleeRepository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public MeleeAccess(MeleeRepository meleeRepository, ApplicationEventPublisher events, Clock clock) {
        this.meleeRepository = meleeRepository;
        this.events = events;
        this.clock = clock;
    }

    public Melee forClub(long meleeId, long clubId) {
        return meleeRepository.findByIdAndClubId(meleeId, clubId).orElseThrow(ApiException::notFound);
    }

    /** Every service calls this after changing anything in a melee: new revision, activity time, live update. */
    public void recordChange(Melee melee) {
        meleeRepository.recordChange(melee.getId(), clock.instant());
        events.publishEvent(new MeleeChangedEvent(melee.getId(), melee.getPublicCode()));
    }
}
