package es.arrima.api;

import static es.arrima.TestClubs.json;
import static org.assertj.core.api.Assertions.assertThat;

import es.arrima.IntegrationTest;
import es.arrima.MutableClock;
import es.arrima.TestClubs;
import es.arrima.TestClubs.RegisteredClub;
import es.arrima.TestMelees;
import es.arrima.TestMelees.ScenarioMelee;
import es.arrima.auth.SignupInvitationRepository;
import es.arrima.files.TestImages;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Phase 7. Three prizes: teams 1 and 2 (2 wins) play for 1st-2nd, teams 3 and 4 (1 win) for the
 * 3rd. Team 4 wins the 3rd prize after a tie-break.
 */
@IntegrationTest
class PrizesIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private SignupInvitationRepository invitations;

    @Autowired
    private MutableClock clock;

    private TestMelees melees;
    private RegisteredClub club;
    private ScenarioMelee scenario;
    private long tieBreakRound;

    @BeforeEach
    void playUntilTheEndOfLaInternacional() {
        melees = new TestMelees(mvc);
        club = new TestClubs(mvc, invitations).register("Club Premios");
        scenario = melees.fourTeamsWithTies(club, 3);
        MvcTestResult started = melees.send(club, "POST", "/api/melees/%d/international/start".formatted(scenario.meleeId()), "{}");
        long lowRound = id(started, "$.international.groups[0].rounds[0].id");
        throwSix(lowRound, 3, "NEAR_JACK", "MISS");
        tieBreakRound = id(throwSix(lowRound, 4, "NEAR_JACK", "MISS"), "$.international.groups[0].rounds[1].id");
        throwSix(tieBreakRound, 3, "OUT", "MISS");
        MvcTestResult lowDone = throwSix(tieBreakRound, 4, "ON_JACK", "MISS");
        long topRound = id(lowDone, "$.international.groups[1].rounds[0].id");
        throwSix(topRound, 1, "SMALL_CIRCLE", "HIT");
        throwSix(topRound, 2, "ON_JACK", "CARREAU");
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void theCeremonyHasThePrizesWithTheirTeamsAndPoints() {
        MvcTestResult result = startPrizes(false);

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("PRIZES");
        List<Integer> positions = json(result, "$.prizes[*].position");
        List<Integer> teams = json(result, "$.prizes[*].teamId");
        List<Integer> points = json(result, "$.prizes[*].points");
        assertThat(positions).containsExactly(1, 2, 3);
        assertThat(teams).containsExactly((int) scenario.team(2), (int) scenario.team(1), (int) scenario.team(4));
        assertThat(points).containsExactly(30, 9, 9);
    }

    @Test
    void spectatorsSeeEachPrizeOnceItIsHandedOut() {
        MvcTestResult started = startPrizes(false);
        String code = json(started, "$.publicCode");
        long thirdPrize = id(started, "$.prizes[2].id");

        assertThat(publicView(code)).bodyJson().extractingPath("$.prizes").asArray().isEmpty();

        melees.send(club, "POST", "/api/melees/%d/prizes/%d/awarded".formatted(scenario.meleeId(), thirdPrize), null);

        assertThat(publicView(code)).bodyJson().extractingPath("$.prizes.length()").isEqualTo(1);
        assertThat(publicView(code)).bodyJson().extractingPath("$.prizes[0].position").isEqualTo(3);
    }

    @Test
    void aPhotoOfTheTeamWithItsPrizeIsStoredAndServed() {
        long prize = id(startPrizes(false), "$.prizes[2].id");
        byte[] photo = TestImages.jpeg(64, 48);

        MvcTestResult uploaded = uploadPhoto(prize, photo);

        assertThat(uploaded).hasStatusOk();
        String url = json(uploaded, "$.prizes[2].photos[0].url");
        MvcTestResult image = mvc.get().uri(url).exchange();
        assertThat(image).hasStatusOk();
        assertThat(image.getResponse().getContentAsByteArray()).isEqualTo(photo);

        long photoId = id(uploaded, "$.prizes[2].photos[0].id");
        assertThat(melees.send(club, "DELETE", "/api/melees/%d/prizes/%d/photos/%d".formatted(scenario.meleeId(), prize, photoId), null))
                .bodyJson().extractingPath("$.prizes[2].photos").asArray().isEmpty();
    }

    @Test
    void theMeleeClosesByItselfAfterTwentyMinutesWithoutActivity() {
        String code = json(startPrizes(false), "$.publicCode");

        // (Through the public view: by now the admin's 15-minute access token has expired.)
        clock.advance(Duration.ofMinutes(19));
        assertThat(publicView(code)).bodyJson().extractingPath("$.status").isEqualTo("PRIZES");

        clock.advance(Duration.ofMinutes(2));
        // Checked on the next request: here, a spectator's.
        assertThat(publicView(code)).bodyJson().extractingPath("$.status").isEqualTo("CLOSED");
        // Closed: spectators now see every prize.
        assertThat(publicView(code)).bodyJson().extractingPath("$.prizes.length()").isEqualTo(3);
    }

    @Test
    void theAdminClosesTheMeleeAndItStaysInTheHistoryReadOnly() {
        long firstPrize = id(startPrizes(false), "$.prizes[0].id");

        assertThat(melees.send(club, "POST", "/api/melees/%d/close".formatted(scenario.meleeId()), null))
                .bodyJson().extractingPath("$.status").isEqualTo("CLOSED");
        assertThat(uploadPhoto(firstPrize, TestImages.jpeg(8, 8))).hasStatus(HttpStatus.CONFLICT);
        assertThat(melees.send(club, "GET", "/api/melees", null)).bodyJson()
                .extractingPath("$[0].status").isEqualTo("CLOSED");
        assertThat(melees.send(club, "POST", "/api/melees/%d/back".formatted(scenario.meleeId()), null))
                .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void correctingLaInternacionalAsksBeforeLosingThePhotoOfATeamWithoutPrize() {
        long thirdPrize = id(startPrizes(false), "$.prizes[2].id");
        uploadPhoto(thirdPrize, TestImages.jpeg(16, 16));
        melees.send(club, "POST", "/api/melees/%d/back".formatted(scenario.meleeId()), null);

        // The tie-break is corrected: team 3 wins the 3rd prize instead of team 4.
        for (int ball = 1; ball <= 3; ball++) {
            melees.throwBall(club, scenario.meleeId(), tieBreakRound, scenario.team(3), "POINTING", ball, "ON_JACK");
            melees.throwBall(club, scenario.meleeId(), tieBreakRound, scenario.team(4), "POINTING", ball, "OUT");
        }

        assertThat(startPrizes(false))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.losses.photos").isEqualTo(1);
        MvcTestResult confirmed = startPrizes(true);
        assertThat(confirmed).bodyJson().extractingPath("$.prizes[2].teamId").isEqualTo((int) scenario.team(3));
        assertThat(confirmed).bodyJson().extractingPath("$.prizes[2].photos").asArray().isEmpty();
    }

    private MvcTestResult startPrizes(boolean confirmLosses) {
        return melees.send(club, "POST", "/api/melees/%d/prizes/start".formatted(scenario.meleeId()),
                "{\"confirmLosses\":%s}".formatted(confirmLosses));
    }

    private MvcTestResult uploadPhoto(long prizeId, byte[] content) {
        return mvc.post().uri("/api/melees/%d/prizes/%d/photos".formatted(scenario.meleeId(), prizeId))
                .header(HttpHeaders.AUTHORIZATION, club.bearer())
                .multipart()
                .file(new MockMultipartFile("file", "foto.jpg", "image/jpeg", content))
                .exchange();
    }

    private MvcTestResult publicView(String code) {
        return mvc.get().uri("/api/public/melees/" + code).exchange();
    }

    private MvcTestResult throwSix(long roundId, int teamNumber, String pointing, String shooting) {
        return melees.throwSix(club, scenario.meleeId(), roundId, scenario.team(teamNumber), pointing, shooting);
    }

    private static long id(MvcTestResult result, String path) {
        return ((Number) json(result, path)).longValue();
    }
}
