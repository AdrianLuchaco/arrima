package es.arrima.push;

import es.arrima.melee.Melee;
import es.arrima.melee.MeleeRepository;
import es.arrima.timer.RoundTimeUpEvent;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tells every subscribed phone that a round's time is up. Runs after the end was committed (so
 * exactly once, see TimerService) and in the background: up to a few hundred phones, each its own
 * request, without holding up whoever noticed the end.
 */
@Component
class PushNotifier {

    private static final Logger log = LoggerFactory.getLogger(PushNotifier.class);

    private final PushSubscriptionRepository subscriptions;
    private final MeleeRepository melees;
    private final PushGateway gateway;
    private final JsonMapper json;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    PushNotifier(PushSubscriptionRepository subscriptions, MeleeRepository melees, PushGateway gateway, JsonMapper json) {
        this.subscriptions = subscriptions;
        this.melees = melees;
        this.gateway = gateway;
        this.json = json;
    }

    @TransactionalEventListener(fallbackExecution = true)
    void onTimeUp(RoundTimeUpEvent event) {
        Melee melee = melees.findById(event.meleeId()).orElse(null);
        List<PushSubscription> targets = subscriptions.findByMeleeId(event.meleeId());
        if (melee == null || targets.isEmpty()) {
            return;
        }
        byte[] forAdmin = TimeUpNotification.payload(json, event.roundNumber(), "/melees/" + melee.getId(), melee.getId());
        byte[] forPlayers = TimeUpNotification.payload(json, event.roundNumber(), "/m/" + melee.getPublicCode(), melee.getId());
        targets.forEach(target -> executor.execute(
                () -> deliver(target, target.getAudience() == PushAudience.ADMIN ? forAdmin : forPlayers)));
    }

    /** Notifications already handed over are still sent on shutdown. */
    @PreDestroy
    void finishPending() {
        executor.close();
    }

    private void deliver(PushSubscription subscription, byte[] payload) {
        try {
            if (gateway.send(subscription, payload) == PushGateway.Result.GONE) {
                subscriptions.deleteById(subscription.getId());
            }
        } catch (RuntimeException e) {
            log.warn("Could not send a time-up notification: {}", e.getMessage());
        }
    }
}
