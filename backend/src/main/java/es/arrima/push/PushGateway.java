package es.arrima.push;

/** Delivers one encrypted notification to one subscription (WebPushSender; a fake in tests). */
public interface PushGateway {

    enum Result {
        DELIVERED,
        /** The browser unsubscribed or the subscription expired: it can be deleted. */
        GONE,
        FAILED
    }

    Result send(PushSubscription subscription, byte[] payload);
}
