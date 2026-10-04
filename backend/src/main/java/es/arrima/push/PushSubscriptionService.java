package es.arrima.push;

import es.arrima.melee.Melee;
import es.arrima.melee.MeleeClosedEvent;
import es.arrima.melee.MeleeStatus;
import es.arrima.shared.error.ApiException;
import es.arrima.shared.error.ErrorCode;
import java.time.Clock;
import java.util.Base64;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Avísame cuando se acabe el tiempo". The players' subscription is the only thing anyone can write
 * without an account, so everything is checked: the push service (see PushEndpoints), the keys, the
 * melee (by its code, and still open), how many per melee, and how often per address (the caller
 * applies the rate limit). Every subscription of a melee is deleted when it closes.
 */
@Service
public class PushSubscriptionService {

    static final int MAX_PER_MELEE = 500;
    private static final int AUTH_SECRET_LENGTH = 16;

    private final PushSubscriptionRepository repository;
    private final PushKeys pushKeys;
    private final Clock clock;

    public PushSubscriptionService(PushSubscriptionRepository repository, PushKeys pushKeys, Clock clock) {
        this.repository = repository;
        this.pushKeys = pushKeys;
        this.clock = clock;
    }

    @Transactional
    public void subscribe(Melee melee, NewPushSubscription subscription, PushAudience audience) {
        if (pushKeys.keys().isEmpty()) {
            throw ApiException.notFound();
        }
        if (melee.getStatus() == MeleeStatus.CLOSED) {
            throw new ApiException(ErrorCode.INVALID_STATE, Map.of("status", melee.getStatus().name()));
        }
        validate(subscription);
        repository.findByMeleeIdAndEndpoint(melee.getId(), subscription.endpoint()).ifPresentOrElse(
                existing -> existing.renew(subscription, audience),
                () -> {
                    if (repository.countByMeleeId(melee.getId()) >= MAX_PER_MELEE) {
                        throw new ApiException(ErrorCode.PUSH_LIMIT, Map.of("max", MAX_PER_MELEE));
                    }
                    repository.save(new PushSubscription(melee.getId(), subscription, audience, clock.instant()));
                });
    }

    /** Within the closing transaction: no notification is meant for a closed melee. */
    @EventListener
    void onMeleeClosed(MeleeClosedEvent event) {
        repository.deleteByMeleeId(event.meleeId());
    }

    private static void validate(NewPushSubscription subscription) {
        if (PushEndpoints.parseAllowed(subscription.endpoint()).isEmpty()) {
            throw ApiException.validation("endpoint", "NotAllowed");
        }
        try {
            P256.publicKey(Base64.getUrlDecoder().decode(subscription.p256dh()));
        } catch (IllegalArgumentException e) {
            throw ApiException.validation("keys.p256dh", "Invalid");
        }
        try {
            if (Base64.getUrlDecoder().decode(subscription.auth()).length != AUTH_SECRET_LENGTH) {
                throw ApiException.validation("keys.auth", "Invalid");
            }
        } catch (IllegalArgumentException e) {
            throw ApiException.validation("keys.auth", "Invalid");
        }
    }
}
