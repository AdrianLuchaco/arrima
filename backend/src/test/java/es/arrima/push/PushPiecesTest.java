package es.arrima.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import java.net.URI;
import java.security.interfaces.ECPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.web.client.RestClient;

/** The parts of Web Push that need no database: endpoints, VAPID signature, and the HTTP request. */
class PushPiecesTest {

    private static final Instant NOW = Instant.parse("2026-10-04T10:00:00Z");
    private static final String FCM = "https://fcm.googleapis.com/fcm/send/abc123";

    @Test
    void onlyTheBrowsersPushServicesAreAccepted() {
        assertThat(PushEndpoints.parseAllowed(FCM)).isPresent();
        assertThat(PushEndpoints.parseAllowed("https://web.push.apple.com/QGuQyavXutnMH")).isPresent();
        assertThat(PushEndpoints.parseAllowed("https://updates.push.services.mozilla.com/wpush/v2/gAAA")).isPresent();
        assertThat(PushEndpoints.parseAllowed("https://wns2-db5p.notify.windows.com/w/?token=x")).isPresent();

        assertThat(PushEndpoints.parseAllowed("http://fcm.googleapis.com/fcm/send/x")).isEmpty();
        assertThat(PushEndpoints.parseAllowed("https://localhost/x")).isEmpty();
        assertThat(PushEndpoints.parseAllowed("https://169.254.169.254/latest/meta-data")).isEmpty();
        assertThat(PushEndpoints.parseAllowed("https://fcm.googleapis.com.evil.example/x")).isEmpty();
        assertThat(PushEndpoints.parseAllowed("https://user@fcm.googleapis.com/x")).isEmpty();
        assertThat(PushEndpoints.parseAllowed("https://fcm.googleapis.com:8443/x")).isEmpty();
        assertThat(PushEndpoints.parseAllowed("https://fcm.googleapis.com/" + "x".repeat(1000))).isEmpty();
        assertThat(PushEndpoints.parseAllowed("no es una url")).isEmpty();
    }

    @Test
    void theVapidTokenIsSignedForThatPushServiceAndShortLived() throws Exception {
        VapidKeys keys = VapidKeys.generate();

        String header = VapidAuthorization.header(URI.create(FCM), keys, "mailto:club@example.com", NOW);

        assertThat(header).startsWith("vapid t=").endsWith(", k=" + keys.publicKeyBase64Url());
        SignedJWT jwt = SignedJWT.parse(header.substring("vapid t=".length(), header.indexOf(',')));
        assertThat(jwt.verify(new ECDSAVerifier(keys.publicKey()))).isTrue();
        assertThat(jwt.getHeader().getAlgorithm().getName()).isEqualTo("ES256");
        assertThat(jwt.getJWTClaimsSet().getAudience()).containsExactly("https://fcm.googleapis.com");
        assertThat(jwt.getJWTClaimsSet().getSubject()).isEqualTo("mailto:club@example.com");
        assertThat(jwt.getJWTClaimsSet().getExpirationTime().toInstant()).isBetween(NOW, NOW.plusSeconds(24 * 3600));
    }

    @Test
    void sendsTheEncryptedNotificationWithTheHeadersOfTheStandards() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer pushService = MockRestServiceServer.bindTo(builder).build();
        WebPushSender sender = new WebPushSender(builder, VapidKeys.generate(), "mailto:club@example.com",
                Clock.fixed(NOW, ZoneOffset.UTC));

        pushService.expect(requestTo(FCM))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Encoding", "aes128gcm"))
                .andExpect(header("TTL", "1800"))
                .andExpect(header("Urgency", "high"))
                .andExpect(MockRestRequestMatchers.header("Authorization", org.hamcrest.Matchers.startsWith("vapid t=")))
                .andRespond(withStatus(HttpStatus.CREATED));

        assertThat(sender.send(subscription(FCM), "{}".getBytes())).isEqualTo(PushGateway.Result.DELIVERED);
        pushService.verify();
    }

    @Test
    void aSubscriptionThatNoLongerExistsIsReportedGone() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer pushService = MockRestServiceServer.bindTo(builder).build();
        WebPushSender sender = new WebPushSender(builder, VapidKeys.generate(), "mailto:club@example.com",
                Clock.fixed(NOW, ZoneOffset.UTC));
        pushService.expect(requestTo(FCM)).andRespond(withStatus(HttpStatus.GONE));

        assertThat(sender.send(subscription(FCM), "{}".getBytes())).isEqualTo(PushGateway.Result.GONE);
    }

    @Test
    void neverSendsAnywhereElseWhateverIsStored() {
        WebPushSender sender = new WebPushSender(RestClient.builder(), VapidKeys.generate(), "mailto:club@example.com",
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(sender.send(subscription("https://internal.example/admin"), "{}".getBytes()))
                .isEqualTo(PushGateway.Result.GONE);
    }

    private static PushSubscription subscription(String endpoint) {
        String browserKey = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(P256.encode((ECPublicKey) P256.newKeyPair().getPublic()));
        String auth = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16]);
        return new PushSubscription(1, new NewPushSubscription(endpoint, browserKey, auth), PushAudience.PUBLIC, NOW);
    }
}
