package es.arrima.timer;

import static es.arrima.TestClubs.json;
import static org.assertj.core.api.Assertions.assertThat;

import es.arrima.IntegrationTest;
import es.arrima.MutableClock;
import es.arrima.RecordingPushGateway;
import es.arrima.TestClubs;
import es.arrima.TestClubs.RegisteredClub;
import es.arrima.TestMelees;
import es.arrima.auth.SignupInvitationRepository;
import es.arrima.push.VapidKeys;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** "Empezar partida": the countdown of a round, its alarm notifications and how they survive a restart. */
@IntegrationTest
class TimerIntegrationTest {

    private static final long MATCH_MILLIS = Duration.ofMinutes(45).toMillis();

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private SignupInvitationRepository invitations;

    @Autowired
    private MutableClock clock;

    @Autowired
    private RecordingPushGateway pushGateway;

    @Autowired
    private TimerService timerService;

    @Autowired
    private TimerTriggers triggers;

    private TestMelees melees;
    private TestClubs clubs;
    private RegisteredClub club;
    private long meleeId;
    private String code;

    @BeforeEach
    void eightPlayersWithTheirCourtSchedule() {
        melees = new TestMelees(mvc);
        clubs = new TestClubs(mvc, invitations);
        club = clubs.register("Club Reloj");
        meleeId = melees.create(club, 2);
        melees.signUp(club, meleeId, 8);
        melees.send(club, "POST", "/api/melees/%d/teams/draw".formatted(meleeId), "{}");
        MvcTestResult schedule = melees.send(club, "POST", "/api/melees/%d/schedule/generate".formatted(meleeId), "{}");
        code = json(schedule, "$.publicCode");
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void startingARoundStartsItsCountdownForEveryone() {
        MvcTestResult started = start(1, false);

        assertThat(started).hasStatusOk().bodyJson().extractingPath("$.rounds[0].timer.state").isEqualTo("RUNNING");
        assertThat(started).bodyJson().extractingPath("$.rounds[0].timer.durationMillis").isEqualTo((int) MATCH_MILLIS);
        assertThat(Duration.between(startedAt(started, 0), endsAt(started, 0))).isEqualTo(Duration.ofMinutes(45));
        // Spectators see it too.
        assertThat(publicView()).bodyJson().extractingPath("$.rounds[0].timer.state").isEqualTo("RUNNING");
        assertThat(started).bodyJson().extractingPath("$.rounds[1].timer").isNull();
    }

    @Test
    void theMatchLengthComesFromTheMelee() {
        long shortMelee = melees.create(club, 2);
        melees.send(club, "PUT", "/api/melees/%d/settings".formatted(shortMelee),
                "{\"courtCount\":8,\"roundsCount\":3,\"prizeCount\":5,\"entryFeeCents\":0,\"matchMinutes\":30}");
        melees.signUp(club, shortMelee, 8);
        melees.send(club, "POST", "/api/melees/%d/teams/draw".formatted(shortMelee), "{}");
        melees.send(club, "POST", "/api/melees/%d/schedule/generate".formatted(shortMelee), "{}");

        MvcTestResult started = melees.send(club, "POST", "/api/melees/%d/timer/rounds/1/start".formatted(shortMelee), "{}");

        assertThat(started).bodyJson().extractingPath("$.rounds[0].timer.durationMillis").isEqualTo(30 * 60_000);
    }

    @Test
    void aPauseStopsTheClockAndResumingMovesTheEndBack() {
        Instant originalEnd = endsAt(start(1, false), 0);
        clock.advance(Duration.ofMinutes(10));
        club = clubs.login(club);

        MvcTestResult paused = action("pause", 1);
        assertThat(paused).bodyJson().extractingPath("$.rounds[0].timer.state").isEqualTo("PAUSED");

        clock.advance(Duration.ofMinutes(5));
        club = clubs.login(club);

        MvcTestResult resumed = action("resume", 1);

        assertThat(resumed).bodyJson().extractingPath("$.rounds[0].timer.state").isEqualTo("RUNNING");
        assertThat(endsAt(resumed, 0)).isEqualTo(originalEnd.plus(Duration.ofMinutes(5)));
    }

    @Test
    void aCountdownStartedByMistakeCanBeCancelledAndStartedAgain() {
        start(1, false);

        assertThat(melees.send(club, "DELETE", "/api/melees/%d/timer/rounds/1".formatted(meleeId), null))
                .hasStatusOk().bodyJson().extractingPath("$.rounds[0].timer").isNull();
        assertThat(start(1, false)).hasStatusOk();
    }

    @Test
    void startingTheNextRoundAsksBeforeStoppingTheCurrentCountdown() {
        start(1, false);
        clock.advance(Duration.ofMinutes(40));
        club = clubs.login(club);

        assertThat(start(2, false)).hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("TIMER_RUNNING");

        MvcTestResult second = start(2, true);
        assertThat(second).bodyJson().extractingPath("$.rounds[0].timer.state").isEqualTo("ENDED");
        assertThat(second).bodyJson().extractingPath("$.rounds[0].timer.endReason").isEqualTo("NEXT_ROUND");
        assertThat(second).bodyJson().extractingPath("$.rounds[1].timer.state").isEqualTo("RUNNING");
    }

    @Test
    void whenTheTimeIsUpEverySubscribedPhoneIsToldOnce() {
        String player = subscribePlayer();
        String admin = subscribeAdmin();
        start(1, false);

        clock.advance(Duration.ofMinutes(45).plusSeconds(1));
        club = clubs.login(club);

        // Several triggers at once: the admin's phone, a spectator looking, the safety net...
        runTogether(() -> melees.send(club, "POST", "/api/melees/%d/timer/check".formatted(meleeId), null),
                this::publicView,
                () -> {
                    timerService.expireAllDue();
                    return null;
                },
                () -> {
                    timerService.expireIfDue(meleeId);
                    return null;
                });

        List<RecordingPushGateway.Sent> sent = pushGateway.awaitFor(meleeId, 2);
        assertThat(sent).extracting(RecordingPushGateway.Sent::endpoint).containsExactlyInAnyOrder(player, admin);
        assertThat(sent).allSatisfy(notification ->
                assertThat(notification.payload()).contains("¡Tiempo! Partida 1 terminada"));
        assertThat(sent).filteredOn(notification -> notification.endpoint().equals(player))
                .singleElement().satisfies(notification -> assertThat(notification.payload()).contains("/m/" + code));
        MvcTestResult view = melees.get(club, meleeId);
        assertThat(view).bodyJson().extractingPath("$.rounds[0].timer.endReason").isEqualTo("TIME_UP");
        // Recorded at the moment the time ran out, not when it was noticed.
        assertThat(endedAt(view, 0)).isEqualTo(endsAt(view, 0));
    }

    @Test
    void theServerNeverEndsACountdownEarly() {
        subscribePlayer();
        start(1, false);
        clock.advance(Duration.ofMinutes(44));
        club = clubs.login(club);

        assertThat(melees.send(club, "POST", "/api/melees/%d/timer/check".formatted(meleeId), null))
                .bodyJson().extractingPath("$.rounds[0].timer.state").isEqualTo("RUNNING");
        assertThat(pushGateway.awaitFor(meleeId, 1)).isEmpty();
    }

    @Test
    void aPausedCountdownNeverRunsOut() {
        subscribePlayer();
        start(1, false);
        clock.advance(Duration.ofMinutes(30));
        club = clubs.login(club);
        action("pause", 1);

        clock.advance(Duration.ofHours(2));
        club = clubs.login(club);


        assertThat(publicView()).bodyJson().extractingPath("$.rounds[0].timer.state").isEqualTo("PAUSED");
        assertThat(pushGateway.awaitFor(meleeId, 1)).isEmpty();
    }

    @Test
    void aCountdownThatRanOutWhileTheServerWasDownIsRecordedAtStartUp() {
        subscribePlayer();
        start(1, false);
        // The backend restarts after the end: the task scheduled in memory is gone.
        clock.advance(Duration.ofMinutes(50));
        club = clubs.login(club);

        triggers.recoverAfterRestart();

        assertThat(pushGateway.awaitFor(meleeId, 1)).hasSize(1);
        assertThat(melees.get(club, meleeId)).bodyJson().extractingPath("$.rounds[0].timer.endReason").isEqualTo("TIME_UP");
    }

    @Test
    void theCountdownsBelongToTheCourtSchedule() {
        start(1, false);

        melees.send(club, "POST", "/api/melees/%d/back".formatted(meleeId), null);
        MvcTestResult regenerated = melees.send(club, "POST", "/api/melees/%d/schedule/generate".formatted(meleeId),
                "{\"confirmLosses\":true}");

        assertThat(regenerated).bodyJson().extractingPath("$.rounds[0].timer").isNull();
    }

    @Test
    void onlyDuringTheMatchesAndOnlyRoundsThatExist() {
        assertThat(start(4, false)).hasStatus(HttpStatus.BAD_REQUEST);
        long otherMelee = melees.create(club, 2);
        assertThat(melees.send(club, "POST", "/api/melees/%d/timer/rounds/1/start".formatted(otherMelee), "{}"))
                .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void anotherClubCannotTouchTheCountdown() {
        start(1, false);
        RegisteredClub intruder = new TestClubs(mvc, invitations).register("Club Intruso");

        assertThat(melees.send(intruder, "POST", "/api/melees/%d/timer/rounds/1/pause".formatted(meleeId), null))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(melees.send(intruder, "DELETE", "/api/melees/%d/timer/rounds/1".formatted(meleeId), null))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(melees.send(intruder, "POST", "/api/melees/%d/timer/rounds/2/start".formatted(meleeId), "{}"))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void theServerTellsItsTime() {
        assertThat(mvc.get().uri("/api/time")).hasStatusOk()
                .bodyJson().extractingPath("$.now").isEqualTo(clock.millis());
    }

    private MvcTestResult start(int round, boolean stopRunning) {
        return melees.send(club, "POST", "/api/melees/%d/timer/rounds/%d/start".formatted(meleeId, round),
                "{\"stopRunningCountdown\":%s}".formatted(stopRunning));
    }

    private MvcTestResult action(String name, int round) {
        return melees.send(club, "POST", "/api/melees/%d/timer/rounds/%d/%s".formatted(meleeId, round, name), null);
    }

    private MvcTestResult publicView() {
        return mvc.get().uri("/api/public/melees/" + code).exchange();
    }

    private String subscribePlayer() {
        String endpoint = "https://fcm.googleapis.com/fcm/send/" + UUID.randomUUID();
        assertThat(mvc.post().uri("/api/public/melees/%s/push-subscriptions".formatted(code))
                .header("X-Forwarded-For", TestClubs.randomIp())
                .contentType(MediaType.APPLICATION_JSON).content(subscription(endpoint)))
                .hasStatus(HttpStatus.NO_CONTENT);
        return endpoint;
    }

    private String subscribeAdmin() {
        String endpoint = "https://web.push.apple.com/" + UUID.randomUUID();
        assertThat(melees.send(club, "POST", "/api/melees/%d/push-subscriptions".formatted(meleeId), subscription(endpoint)))
                .hasStatus(HttpStatus.NO_CONTENT);
        return endpoint;
    }

    /** As a browser would send it: a real P-256 public key and a 16-byte secret. */
    static String subscription(String endpoint) {
        String auth = Base64.getUrlEncoder().withoutPadding().encodeToString(UUID.randomUUID().toString().substring(0, 16).getBytes());
        return """
                {"endpoint":"%s","expirationTime":null,"keys":{"p256dh":"%s","auth":"%s"}}"""
                .formatted(endpoint, VapidKeys.generate().publicKeyBase64Url(), auth);
    }

    private static void runTogether(Callable<?>... tasks) {
        try (var executor = Executors.newFixedThreadPool(tasks.length)) {
            List<Callable<Object>> all = new ArrayList<>();
            for (Callable<?> task : tasks) {
                all.add(task::call);
            }
            for (var future : executor.invokeAll(all)) {
                future.get();
            }
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static Instant startedAt(MvcTestResult view, int round) {
        return Instant.parse(json(view, "$.rounds[%d].timer.startedAt".formatted(round)));
    }

    private static Instant endsAt(MvcTestResult view, int round) {
        return Instant.parse(json(view, "$.rounds[%d].timer.endsAt".formatted(round)));
    }

    private static Instant endedAt(MvcTestResult view, int round) {
        return Instant.parse(json(view, "$.rounds[%d].timer.endedAt".formatted(round)));
    }
}
