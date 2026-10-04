package es.arrima.push;

/**
 * What the browser gives when subscribing (PushSubscription.toJSON()): where to send the
 * notifications and the keys to encrypt them for that browser only.
 */
public record NewPushSubscription(String endpoint, String p256dh, String auth) {
}
