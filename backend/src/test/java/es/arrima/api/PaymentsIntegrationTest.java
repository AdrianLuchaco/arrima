package es.arrima.api;

import static es.arrima.TestClubs.json;
import static org.assertj.core.api.Assertions.assertThat;

import es.arrima.IntegrationTest;
import es.arrima.TestClubs;
import es.arrima.TestClubs.RegisteredClub;
import es.arrima.TestMelees;
import es.arrima.auth.SignupInvitationRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Payment control at the sign-up table: who paid plays, who did not stays on the list. */
@IntegrationTest
class PaymentsIntegrationTest {

    private static final int FEE = 500;

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private SignupInvitationRepository invitations;

    private TestMelees melees;
    private RegisteredClub club;
    private long meleeId;
    private List<Long> players;

    @BeforeEach
    void eightPeopleSignUpForAMeleeWithAFee() {
        melees = new TestMelees(mvc);
        club = new TestClubs(mvc, invitations).register("Club Cuotas");
        meleeId = melees.create(club, 2, FEE);
        players = ids(melees.signUp(club, meleeId, 8));
    }

    @Test
    void everybodyStartsUnmarkedAndTheTableKeepsCount() {
        assertThat(view()).bodyJson().extractingPath("$.participants[0].paymentStatus").isEqualTo("UNMARKED");

        pay(0, "PAID");
        pay(1, "PAID");
        MvcTestResult result = pay(2, "UNPAID");

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.payments").isEqualTo(Map.of(
                "expected", 8, "paid", 2, "unpaid", 1, "unmarked", 5, "entryFeeCents", FEE, "collectedCents", 1000));
        // The team plan counts only who will play.
        assertThat(result).bodyJson().extractingPath("$.teamPlan.players").isEqualTo(2);
    }

    @Test
    void aPaymentCanBeCorrectedAndUnmarked() {
        pay(0, "UNPAID");
        pay(0, "PAID");

        assertThat(pay(0, "UNMARKED")).bodyJson().extractingPath("$.participants[0].paymentStatus").isEqualTo("UNMARKED");
    }

    @Test
    void theDrawFirstAsksAboutWhoIsStillUnmarked() {
        payFirst(6);

        MvcTestResult refused = draw(false, false);

        assertThat(refused).hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("UNMARKED_PAYMENTS");
        List<Integer> unmarked = json(refused, "$.unmarkedPlayers");
        assertThat(unmarked).containsExactly(players.get(6).intValue(), players.get(7).intValue());
        assertThat(view()).bodyJson().extractingPath("$.status").isEqualTo("REGISTRATION");
    }

    @Test
    void onceConfirmedTheUnmarkedDidNotPayAndOnlyThoseWhoPaidAreDrawn() {
        payFirst(6);

        MvcTestResult drawn = draw(false, true);

        assertThat(drawn).hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("TEAMS");
        assertThat(drawn).bodyJson().extractingPath("$.teams.length()").isEqualTo(3);
        List<Integer> inTeams = json(drawn, "$.teams[*].memberIds[*]");
        assertThat(inTeams).hasSize(6).doesNotContain(players.get(6).intValue(), players.get(7).intValue());
        // They are still on the list, marked as not paid, and spectators see them as not playing.
        assertThat(drawn).bodyJson().extractingPath("$.participants.length()").isEqualTo(8);
        assertThat(drawn).bodyJson().extractingPath("$.participants[7].paymentStatus").isEqualTo("UNPAID");
        assertThat(drawn).bodyJson().extractingPath("$.participants[7].notPlaying").isEqualTo(true);
        assertThat(drawn).bodyJson().extractingPath("$.participants[0].notPlaying").isEqualTo(false);
    }

    @Test
    void whetherThePlayersFitIsDecidedOnThoseWhoPaid() {
        payFirst(5);

        MvcTestResult doesNotFit = draw(false, true);

        assertThat(doesNotFit).hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("TEAMS_DO_NOT_FIT");
        // The draw did not happen, so nobody was recorded as not paid either.
        assertThat(view()).bodyJson().extractingPath("$.payments.unmarked").isEqualTo(3);
        assertThat(draw(true, true)).hasStatusOk().bodyJson().extractingPath("$.teams.length()").isEqualTo(2);
    }

    @Test
    void changingAPaymentAfterTheDrawIsSolvedLikeAWithdrawal() {
        payFirst(6);
        MvcTestResult drawn = draw(false, true);
        long leaving = players.get(0);

        pay(0, "UNPAID");
        MvcTestResult latePayer = pay(6, "PAID");

        assertThat(latePayer).bodyJson().extractingPath("$.teamIssues.membersNotPlaying").isEqualTo(List.of((int) leaving));
        assertThat(latePayer).bodyJson().extractingPath("$.teamIssues.unassignedPlayers")
                .isEqualTo(List.of(players.get(6).intValue()));
        MvcTestResult substituted = melees.send(club, "POST", "/api/melees/%d/teams/substitute".formatted(meleeId),
                "{\"leavingPlayerId\":%d,\"joiningPlayerId\":%d}".formatted(leaving, players.get(6)));
        assertThat(substituted).hasStatusOk().bodyJson().extractingPath("$.teamIssues.membersNotPlaying").asArray().isEmpty();
        assertThat(json(drawn, "$.status").toString()).isEqualTo("TEAMS");
    }

    @Test
    void someoneWhoDidNotPayCannotReplaceAPlayer() {
        payFirst(6);
        draw(false, true);
        pay(0, "UNPAID");

        assertThat(melees.send(club, "POST", "/api/melees/%d/teams/substitute".formatted(meleeId),
                "{\"leavingPlayerId\":%d,\"joiningPlayerId\":%d}".formatted(players.get(0), players.get(7))))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.fields.joiningPlayerId").isEqualTo("NotAvailable");
    }

    @Test
    void paymentsAreFinalOnceTheCourtScheduleExists() {
        payFirst(8);
        draw(false, false);
        melees.send(club, "POST", "/api/melees/%d/schedule/generate".formatted(meleeId), "{}");

        assertThat(pay(0, "UNPAID")).hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("INVALID_STATE");
    }

    @Test
    void theFeeCanOnlyChangeBeforeTheDraw() {
        assertThat(changeFee(300)).hasStatusOk().bodyJson().extractingPath("$.settings.entryFeeCents").isEqualTo(300);

        payFirst(8);
        draw(false, false);

        assertThat(changeFee(500)).hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void someoneWhoDidNotComeIsNotAskedToPay() {
        melees.send(club, "POST", "/api/melees/%d/participants/%d/withdraw".formatted(meleeId, players.get(7)), null);
        payFirst(7);

        assertThat(pay(7, "PAID")).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.fields.participantId").isEqualTo("Withdrawn");
        // Nobody is left unmarked: the draw goes ahead without asking.
        assertThat(draw(true, false)).hasStatusOk();
    }

    @Test
    void spectatorsNeverSeeWhoHasPaid() {
        payFirst(4);
        String code = json(view(), "$.publicCode");

        MvcTestResult publicView = mvc.get().uri("/api/public/melees/" + code).exchange();

        assertThat(publicView).bodyJson().extractingPath("$.payments").isNull();
        assertThat(publicView).bodyJson().extractingPath("$.participants[0].paymentStatus").isNull();
        assertThat(publicView).bodyJson().extractingPath("$.participants[7].notPlaying").isEqualTo(false);
        // Not even the count of those who paid: the plan counts everybody signed up.
        assertThat(publicView).bodyJson().extractingPath("$.teamPlan.players").isEqualTo(8);
    }

    @Test
    void withoutAFeeThereIsNoPaymentControlAtAll() {
        long freeMelee = melees.create(club, 2, 0);
        List<Long> people = ids(melees.signUp(club, freeMelee, 4));

        MvcTestResult view = melees.get(club, freeMelee);
        assertThat(view).bodyJson().extractingPath("$.payments").isNull();
        assertThat(view).bodyJson().extractingPath("$.participants[0].paymentStatus").isNull();
        assertThat(melees.send(club, "PUT", "/api/melees/%d/participants/%d/payment".formatted(freeMelee, people.get(0)),
                "{\"paymentStatus\":\"PAID\"}")).hasStatus(HttpStatus.CONFLICT);
        // Everybody plays, exactly as before the payment control existed.
        assertThat(melees.send(club, "POST", "/api/melees/%d/teams/draw".formatted(freeMelee), "{}"))
                .hasStatusOk().bodyJson().extractingPath("$.teams.length()").isEqualTo(2);
    }

    @Test
    void anotherClubCannotRecordPayments() {
        RegisteredClub intruder = new TestClubs(mvc, invitations).register("Club Intruso");

        assertThat(melees.send(intruder, "PUT", "/api/melees/%d/participants/%d/payment".formatted(meleeId, players.get(0)),
                "{\"paymentStatus\":\"PAID\"}")).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(view()).bodyJson().extractingPath("$.participants[0].paymentStatus").isEqualTo("UNMARKED");
    }

    @Test
    void anUnknownPaymentStatusIsRejected() {
        assertThat(melees.send(club, "PUT", "/api/melees/%d/participants/%d/payment".formatted(meleeId, players.get(0)),
                "{\"paymentStatus\":\"MAYBE\"}")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    private void payFirst(int count) {
        for (int i = 0; i < count; i++) {
            pay(i, "PAID");
        }
    }

    private MvcTestResult pay(int index, String status) {
        return melees.send(club, "PUT", "/api/melees/%d/participants/%d/payment".formatted(meleeId, players.get(index)),
                "{\"paymentStatus\":\"%s\"}".formatted(status));
    }

    private MvcTestResult draw(boolean acceptDifferentTeam, boolean unmarkedDidNotPay) {
        return melees.send(club, "POST", "/api/melees/%d/teams/draw".formatted(meleeId),
                "{\"acceptDifferentTeam\":%s,\"unmarkedDidNotPay\":%s}".formatted(acceptDifferentTeam, unmarkedDidNotPay));
    }

    private MvcTestResult changeFee(int cents) {
        return melees.send(club, "PUT", "/api/melees/%d/settings".formatted(meleeId),
                "{\"courtCount\":8,\"roundsCount\":3,\"prizeCount\":5,\"entryFeeCents\":%d}".formatted(cents));
    }

    private MvcTestResult view() {
        return melees.get(club, meleeId);
    }

    private static List<Long> ids(MvcTestResult result) {
        List<Number> ids = json(result, "$.participants[*].id");
        return ids.stream().map(Number::longValue).toList();
    }
}
