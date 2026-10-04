package es.arrima.live;

import es.arrima.melee.MeleeChangedEvent;
import es.arrima.shared.error.ApiException;
import es.arrima.shared.error.ErrorCode;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Live updates with Server-Sent Events: every phone looking at a melee keeps one connection open,
 * and when anything changes they all receive a short "changed" event and fetch the new view (with
 * its ETag, so an unchanged view costs a 304). If the connection drops, the browser reconnects by
 * itself and the frontend polls meanwhile.
 * <p>
 * In memory: the backend runs as a single instance. Limits protect the 512 MB of the free plan.
 */
@Component
public class MeleeEventBroadcaster {

    private static final long CONNECTION_TIMEOUT_MS = Duration.ofMinutes(30).toMillis();
    private static final int MAX_CONNECTIONS = 1000;

    private final Map<Long, Set<SseEmitter>> emittersByMelee = new ConcurrentHashMap<>();
    private final AtomicInteger connections = new AtomicInteger();

    public SseEmitter subscribe(long meleeId) {
        if (connections.incrementAndGet() > MAX_CONNECTIONS) {
            connections.decrementAndGet();
            throw new ApiException(ErrorCode.TOO_MANY_VIEWERS);
        }
        SseEmitter emitter = new SseEmitter(CONNECTION_TIMEOUT_MS);
        emittersByMelee.computeIfAbsent(meleeId, id -> ConcurrentHashMap.newKeySet()).add(emitter);
        Runnable forget = () -> forget(meleeId, emitter);
        emitter.onCompletion(forget);
        emitter.onTimeout(forget);
        emitter.onError(error -> forget.run());
        // A first event right away: proxies start streaming and the client knows it is connected.
        send(meleeId, emitter, SseEmitter.event().name("ready").data("ready"));
        return emitter;
    }

    /** After the commit, so viewers never fetch a view without the change they were told about. */
    @TransactionalEventListener(fallbackExecution = true)
    public void onMeleeChanged(MeleeChangedEvent event) {
        emittersByMelee.getOrDefault(event.meleeId(), Set.of())
                .forEach(emitter -> send(event.meleeId(), emitter, SseEmitter.event().name("changed").data("changed")));
    }

    /**
     * Vercel's proxy closes a stream that stays silent for 120 seconds, and mobile networks drop idle
     * connections: a comment every 20 seconds keeps it alive (browsers ignore comments).
     */
    @Scheduled(fixedRate = 20_000)
    public void heartbeat() {
        emittersByMelee.forEach((meleeId, emitters) ->
                emitters.forEach(emitter -> send(meleeId, emitter, SseEmitter.event().comment("ping"))));
    }

    int connectionCount() {
        return connections.get();
    }

    private void send(long meleeId, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException clientGone) {
            forget(meleeId, emitter);
        }
    }

    private void forget(long meleeId, SseEmitter emitter) {
        Set<SseEmitter> emitters = emittersByMelee.get(meleeId);
        if (emitters != null && emitters.remove(emitter)) {
            connections.decrementAndGet();
            if (emitters.isEmpty()) {
                emittersByMelee.remove(meleeId, emitters);
            }
        }
    }
}
