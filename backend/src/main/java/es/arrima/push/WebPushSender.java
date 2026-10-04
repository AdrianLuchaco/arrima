package es.arrima.push;

import java.net.URI;
import java.time.Clock;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Web Push over HTTP (RFC 8030): encrypted for the browser (RFC 8291), signed for the service (RFC 8292). */
class WebPushSender implements PushGateway {

    private static final Logger log = LoggerFactory.getLogger(WebPushSender.class);
    /** Seconds the push service keeps it for a phone without signal; later, the alarm is old news. */
    private static final String TIME_TO_LIVE = "1800";

    private final RestClient client;
    private final VapidKeys keys;
    private final String subject;
    private final Clock clock;

    WebPushSender(RestClient.Builder builder, VapidKeys keys, String subject, Clock clock) {
        this.client = builder.build();
        this.keys = keys;
        this.subject = subject;
        this.clock = clock;
    }

    @Override
    public Result send(PushSubscription subscription, byte[] payload) {
        // Checked again here: whatever is stored, requests only ever go to the allowed services.
        URI endpoint = PushEndpoints.parseAllowed(subscription.getEndpoint()).orElse(null);
        if (endpoint == null) {
            return Result.GONE;
        }
        byte[] body = WebPushEncryption.encrypt(payload, P256.publicKey(decode(subscription.getP256dh())),
                decode(subscription.getAuth()));
        try {
            client.post()
                    .uri(endpoint)
                    .header("Authorization", VapidAuthorization.header(endpoint, keys, subject, clock.instant()))
                    .header("TTL", TIME_TO_LIVE)
                    .header("Urgency", "high")
                    .header("Content-Encoding", "aes128gcm")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            return Result.DELIVERED;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().isSameCodeAs(HttpStatus.NOT_FOUND) || e.getStatusCode().isSameCodeAs(HttpStatus.GONE)) {
                return Result.GONE;
            }
            log.warn("Push service refused a notification: {}", e.getStatusCode());
            return Result.FAILED;
        } catch (RestClientException e) {
            log.warn("Could not reach a push service: {}", e.getMessage());
            return Result.FAILED;
        }
    }

    private static byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}
