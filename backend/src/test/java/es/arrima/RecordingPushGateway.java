package es.arrima;

import es.arrima.push.PushGateway;
import es.arrima.push.PushSubscription;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Keeps the notifications instead of sending them; a subscription can be made to look "gone". */
public class RecordingPushGateway implements PushGateway {

    public record Sent(long meleeId, String endpoint, String payload) {
    }

    private final BlockingQueue<Sent> sent = new LinkedBlockingQueue<>();
    private volatile String goneEndpoint;

    @Override
    public Result send(PushSubscription subscription, byte[] payload) {
        sent.add(new Sent(subscription.getMeleeId(), subscription.getEndpoint(), new String(payload)));
        return subscription.getEndpoint().equals(goneEndpoint) ? Result.GONE : Result.DELIVERED;
    }

    public void answerGoneFor(String endpoint) {
        this.goneEndpoint = endpoint;
    }

    /** Notifications go out in the background: waits until {@code count} for this melee arrived, then a moment more. */
    public List<Sent> awaitFor(long meleeId, int count) {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        List<Sent> found = new java.util.ArrayList<>();
        try {
            while (found.size() < count && System.nanoTime() < deadline) {
                Sent next = sent.poll(100, TimeUnit.MILLISECONDS);
                if (next != null && next.meleeId() == meleeId) {
                    found.add(next);
                }
            }
            // Anything extra (a second notification would be a bug) arrives right after.
            Sent extra;
            while ((extra = sent.poll(300, TimeUnit.MILLISECONDS)) != null) {
                if (extra.meleeId() == meleeId) {
                    found.add(extra);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return found;
    }
}
