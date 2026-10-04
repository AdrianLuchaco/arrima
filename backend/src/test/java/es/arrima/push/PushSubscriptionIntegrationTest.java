package es.arrima.push;

import static es.arrima.TestClubs.json;
import static org.assertj.core.api.Assertions.assertThat;

import es.arrima.IntegrationTest;
import es.arrima.RecordingPushGateway;
import es.arrima.TestClubs;
import es.arrima.TestClubs.RegisteredClub;
import es.arrima.TestMelees;
import es.arrima.auth.SignupInvitationRepository;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** "Avísame cuando se acabe el tiempo": the only public write, so everything about it is checked. */
@IntegrationTest
class PushSubscriptionIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private SignupInvitationRepository invitations;

    @Autowired
    private PushSubscriptionRepository subscriptions;

    @Autowired
    private RecordingPushGateway pushGateway;

    @Autowired
    private PushNotifier notifier;

    private TestMelees melees;
    private RegisteredClub club;
    private long meleeId;
    private String code;

    @BeforeEach
    void aMeleeBeingPlayed() {
        melees = new TestMelees(mvc);
        club = new TestClubs(mvc, invitations).register("Club Avisos");
        meleeId = melees.create(club, 2);
        code = json(melees.get(club, meleeId), "$.publicCode");
    }

    @Test
    void theBrowserGetsTheServersPublicKey() {
        MvcTestResult key = mvc.get().uri("/api/public/push/key").exchange();

        assertThat(key).hasStatusOk();
        String publicKey = json(key, "$.publicKey");
        assertThat(Base64.getUrlDecoder().decode(publicKey)).hasSize(65);
    }

    @Test
    void aPhoneSubscribesOnceEvenIfItAsksTwice() {
        String endpoint = fcm();

        assertThat(subscribe(code, body(endpoint, validKey(), validAuth()))).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(subscribe(code, body(endpoint, validKey(), validAuth()))).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(subscriptions.findByMeleeId(meleeId)).hasSize(1);
    }

    @Test
    void onlyThePushServicesOfTheBrowsersAreAccepted() {
        for (String endpoint : new String[] {"https://internal.example/admin", "http://fcm.googleapis.com/x",
                "https://127.0.0.1/x", "https://fcm.googleapis.com.attacker.example/x"}) {
            assertThat(subscribe(code, body(endpoint, validKey(), validAuth())))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().extractingPath("$.fields.endpoint").isEqualTo("NotAllowed");
        }
        assertThat(subscriptions.findByMeleeId(meleeId)).isEmpty();
    }

    @Test
    void theKeysMustBeRealOnes() {
        String notOnTheCurve = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[65]);

        assertThat(subscribe(code, body(fcm(), notOnTheCurve, validAuth())))
                .bodyJson().extractingPath("$.fields['keys.p256dh']").isEqualTo("Invalid");
        assertThat(subscribe(code, body(fcm(), validKey(), "c2hvcnQ")))
                .bodyJson().extractingPath("$.fields['keys.auth']").isEqualTo("Invalid");
        assertThat(subscribe(code, "{\"endpoint\":\"" + fcm() + "\"}")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void anUnknownCodeFindsNothing() {
        assertThat(subscribe("ZZZZZZZZ", body(fcm(), validKey(), validAuth()))).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void oneAddressCannotSubscribeWithoutLimit() {
        String ip = TestClubs.randomIp();
        for (int i = 0; i < 20; i++) {
            assertThat(subscribe(code, body(fcm(), validKey(), validAuth()), ip)).hasStatus(HttpStatus.NO_CONTENT);
        }

        assertThat(subscribe(code, body(fcm(), validKey(), validAuth()), ip))
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void closingTheMeleeForgetsEveryPhone() {
        long played = playedUntilThePrizes();
        String playedCode = json(melees.get(club, played), "$.publicCode");
        subscribe(playedCode, body(fcm(), validKey(), validAuth()));

        melees.send(club, "POST", "/api/melees/%d/close".formatted(played), null);

        assertThat(subscriptions.findByMeleeId(played)).isEmpty();
        assertThat(subscribe(playedCode, body(fcm(), validKey(), validAuth()))).hasStatus(HttpStatus.CONFLICT);
    }

    /** The scenario of the prize tests: four teams, la Internacional played, prizes started. */
    private long playedUntilThePrizes() {
        TestMelees.ScenarioMelee scenario = melees.fourTeamsWithTies(club, 3);
        long id = scenario.meleeId();
        MvcTestResult started = melees.send(club, "POST", "/api/melees/%d/international/start".formatted(id), "{}");
        long lowRound = ((Number) json(started, "$.international.groups[0].rounds[0].id")).longValue();
        melees.throwSix(club, id, lowRound, scenario.team(3), "NEAR_JACK", "MISS");
        MvcTestResult tie = melees.throwSix(club, id, lowRound, scenario.team(4), "NEAR_JACK", "MISS");
        long tieBreak = ((Number) json(tie, "$.international.groups[0].rounds[1].id")).longValue();
        melees.throwSix(club, id, tieBreak, scenario.team(3), "OUT", "MISS");
        MvcTestResult lowDone = melees.throwSix(club, id, tieBreak, scenario.team(4), "ON_JACK", "MISS");
        long topRound = ((Number) json(lowDone, "$.international.groups[1].rounds[0].id")).longValue();
        melees.throwSix(club, id, topRound, scenario.team(1), "SMALL_CIRCLE", "HIT");
        melees.throwSix(club, id, topRound, scenario.team(2), "ON_JACK", "CARREAU");
        assertThat(melees.send(club, "POST", "/api/melees/%d/prizes/start".formatted(id), "{}")).hasStatusOk();
        return id;
    }

    @Test
    void aPhoneThatUnsubscribedIsForgottenAfterTheNextNotification() {
        String gone = fcm();
        subscribe(code, body(gone, validKey(), validAuth()));
        subscribe(code, body(fcm(), validKey(), validAuth()));
        pushGateway.answerGoneFor(gone);

        notifier.onTimeUp(new es.arrima.timer.RoundTimeUpEvent(meleeId, 1));

        assertThat(pushGateway.awaitFor(meleeId, 2)).hasSize(2);
        assertThat(subscriptions.findByMeleeId(meleeId)).extracting(PushSubscription::getEndpoint).doesNotContain(gone);
    }

    private MvcTestResult subscribe(String meleeCode, String body) {
        return subscribe(meleeCode, body, TestClubs.randomIp());
    }

    private MvcTestResult subscribe(String meleeCode, String body, String ip) {
        return mvc.post().uri("/api/public/melees/%s/push-subscriptions".formatted(meleeCode))
                .header("X-Forwarded-For", ip)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    private static String body(String endpoint, String p256dh, String auth) {
        return """
                {"endpoint":"%s","expirationTime":null,"keys":{"p256dh":"%s","auth":"%s"}}""".formatted(endpoint, p256dh, auth);
    }

    private static String fcm() {
        return "https://fcm.googleapis.com/fcm/send/" + UUID.randomUUID();
    }

    private static String validKey() {
        return VapidKeys.generate().publicKeyBase64Url();
    }

    private static String validAuth() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(UUID.randomUUID().toString().substring(0, 16).getBytes());
    }
}
