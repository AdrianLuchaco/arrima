package es.arrima.api;

import static es.arrima.TestClubs.json;
import static org.assertj.core.api.Assertions.assertThat;

import es.arrima.IntegrationTest;
import es.arrima.TestClubs;
import es.arrima.TestClubs.RegisteredClub;
import es.arrima.TestMelees;
import es.arrima.auth.SignupInvitationRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Phase 3: creating a melee and the sign-up list. */
@IntegrationTest
class RegistrationIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private SignupInvitationRepository invitations;

    private TestClubs clubs;
    private TestMelees melees;
    private RegisteredClub club;

    @BeforeEach
    void setUp() {
        clubs = new TestClubs(mvc, invitations);
        melees = new TestMelees(mvc);
        club = clubs.register("Club Petanca");
    }

    @Test
    void aNewMeleeIsNamedAfterTodayAndCopiesTheClubSettings() {
        MvcTestResult created = melees.send(club, "POST", "/api/melees", "{\"teamSize\":2}");

        assertThat(created).hasStatus(HttpStatus.CREATED).bodyJson().isLenientlyEqualTo("""
                {"playedOn":"%s","format":"CLASSIC","teamSize":2,"status":"REGISTRATION",
                 "settings":{"courtCount":8,"roundsCount":3,"prizeCount":5,"entryFeeCents":500},
                 "scoring":{"pointingOnJack":5,"shootingCarreau":5},
                 "club":{"name":"Club Petanca"},"participants":[]}"""
                .formatted(LocalDate.now(ZoneId.of("Europe/Madrid"))));
    }

    @Test
    void settingsCanBeAdjustedForTheDay() {
        MvcTestResult result = melees.send(club, "POST", "/api/melees",
                "{\"teamSize\":3,\"courtCount\":5,\"roundsCount\":4,\"prizeCount\":3}");

        assertThat(result).hasStatus(HttpStatus.CREATED).bodyJson().isLenientlyEqualTo("""
                {"teamSize":3,"settings":{"courtCount":5,"roundsCount":4,"prizeCount":3}}""");
    }

    @Test
    void thePreviewOfAWhatsAppListFlagsDuplicatesStruckNamesAndHeaderLines() {
        long meleeId = melees.create(club, 2);
        melees.send(club, "POST", "/api/melees/%d/participants".formatted(meleeId), "{\"name\":\"José García\"}");

        MvcTestResult preview = melees.send(club, "POST", "/api/melees/%d/participants/preview".formatted(meleeId), """
                {"text":"4 de octubre melé\\n1. Manuel\\n2. jose garcia\\n3. ~Antonio~\\n4.\\n5) Paqui 💃"}""");

        assertThat(preview).hasStatusOk().bodyJson().isStrictlyEqualTo("""
                [{"listNumber":4,"name":"de octubre melé","struckThrough":false,"beforeListStart":true,"alreadyRegistered":false},
                 {"listNumber":1,"name":"Manuel","struckThrough":false,"beforeListStart":false,"alreadyRegistered":false},
                 {"listNumber":2,"name":"jose garcia","struckThrough":false,"beforeListStart":false,"alreadyRegistered":true},
                 {"listNumber":3,"name":"Antonio","struckThrough":true,"beforeListStart":false,"alreadyRegistered":false},
                 {"listNumber":5,"name":"Paqui 💃","struckThrough":false,"beforeListStart":false,"alreadyRegistered":false}]""");
        // Nothing was saved by the preview.
        assertThat(melees.get(club, meleeId)).bodyJson().extractingPath("$.participants.length()").isEqualTo(1);
    }

    @Test
    void confirmedPeopleAreSavedInListOrderKeepingTheirNumbers() {
        long meleeId = melees.create(club, 2);

        MvcTestResult result = melees.send(club, "POST", "/api/melees/%d/participants/import".formatted(meleeId), """
                {"participants":[{"listNumber":3,"name":"  Paqui \\u200B"},{"listNumber":1,"name":"Manuel"},
                                 {"listNumber":null,"name":"Sin número"},{"listNumber":2,"name":"Pepe"}]}""");

        List<String> names = json(result, "$.participants[*].name");
        List<Integer> numbers = json(result, "$.participants[*].listNumber");
        assertThat(names).containsExactly("Manuel", "Pepe", "Paqui", "Sin número");
        assertThat(numbers).containsExactly(1, 2, 3, null);
    }

    @Test
    void anImportWithAnInvalidNameIsRejectedWholePointingToTheRow() {
        long meleeId = melees.create(club, 2);

        MvcTestResult result = melees.send(club, "POST", "/api/melees/%d/participants/import".formatted(meleeId), """
                {"participants":[{"listNumber":1,"name":"Manuel"},{"listNumber":2,"name":" \\u200B "}]}""");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.fields['participants[1].name']").isEqualTo("NotBlank");
        assertThat(melees.get(club, meleeId)).bodyJson().extractingPath("$.participants.length()").isEqualTo(0);
    }

    @Test
    void theScreenWarnsWhenThePlayersDoNotFit() {
        long meleeId = melees.create(club, 2);

        MvcTestResult result = melees.signUp(club, meleeId, 25);

        assertThat(result).bodyJson().extractingPath("$.teamPlan").isEqualTo(Map.of(
                "players", 25, "fits", false, "playable", true, "teamCount", 12,
                "teamsBySize", Map.of("2", 11, "3", 1)));
    }

    @Test
    void aWithdrawnPlayerStaysOnTheListButDoesNotCountForTheTeams() {
        long meleeId = melees.create(club, 2);
        long firstId = ((Number) json(melees.signUp(club, meleeId, 25), "$.participants[0].id")).longValue();

        MvcTestResult result = melees.send(club, "POST",
                "/api/melees/%d/participants/%d/withdraw".formatted(meleeId, firstId), null);

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.participants[0].status").isEqualTo("WITHDRAWN");
        assertThat(result).bodyJson().extractingPath("$.participants.length()").isEqualTo(25);
        assertThat(result).bodyJson().extractingPath("$.teamPlan").isEqualTo(Map.of(
                "players", 24, "fits", true, "playable", true, "teamCount", 12, "teamsBySize", Map.of("2", 12)));
    }

    @Test
    void theAdminCanEditAndDeleteSomeoneBeforeTheDraw() {
        long meleeId = melees.create(club, 2);
        long id = ((Number) json(melees.signUp(club, meleeId, 3), "$.participants[1].id")).longValue();

        assertThat(melees.send(club, "PUT", "/api/melees/%d/participants/%d".formatted(meleeId, id),
                "{\"listNumber\":7,\"name\":\"Pepe el de la tienda\"}"))
                .bodyJson().extractingPath("$.participants[2].name").isEqualTo("Pepe el de la tienda");
        assertThat(melees.send(club, "DELETE", "/api/melees/%d/participants/%d".formatted(meleeId, id), null))
                .bodyJson().extractingPath("$.participants.length()").isEqualTo(2);
    }

    @Test
    void theListOfMeleesShowsActivePlayers() {
        long meleeId = melees.create(club, 2);
        melees.signUp(club, meleeId, 10);

        assertThat(melees.send(club, "GET", "/api/melees", null))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("[{\"id\":%d,\"status\":\"REGISTRATION\",\"activePlayers\":10}]".formatted(meleeId));
    }

    @Test
    void aClubCannotSeeOrTouchAnotherClubsMelee() {
        long meleeOfA = melees.create(club, 2);
        melees.signUp(club, meleeOfA, 4);
        long participantOfA = ((Number) json(melees.get(club, meleeOfA), "$.participants[0].id")).longValue();
        RegisteredClub other = clubs.register("Otro club");

        assertThat(melees.send(other, "GET", "/api/melees", null)).bodyJson().isStrictlyEqualTo("[]");
        assertThat(melees.get(other, meleeOfA)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(melees.send(other, "POST", "/api/melees/%d/participants".formatted(meleeOfA), "{\"name\":\"Intruso\"}"))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(melees.send(other, "DELETE", "/api/melees/%d/participants/%d".formatted(meleeOfA, participantOfA), null))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(melees.send(other, "DELETE", "/api/melees/" + meleeOfA, null)).hasStatus(HttpStatus.NOT_FOUND);
        // Nothing changed for its owner.
        assertThat(melees.get(club, meleeOfA)).bodyJson().extractingPath("$.participants.length()").isEqualTo(4);
    }

    @Test
    void aParticipantOfAnotherMeleeOfTheSameClubIsNotFound() {
        long first = melees.create(club, 2);
        long second = melees.create(club, 2);
        long participantOfFirst = ((Number) json(melees.signUp(club, first, 2), "$.participants[0].id")).longValue();

        assertThat(melees.send(club, "POST", "/api/melees/%d/participants/%d/withdraw".formatted(second, participantOfFirst), null))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void theFirstPhaseCannotGoBack() {
        long meleeId = melees.create(club, 2);

        assertThat(melees.send(club, "POST", "/api/melees/%d/back".formatted(meleeId), null))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("INVALID_STATE");
    }

    @Test
    void aDeletedMeleeIsGone() {
        long meleeId = melees.create(club, 2);
        melees.signUp(club, meleeId, 4);

        assertThat(melees.send(club, "DELETE", "/api/melees/" + meleeId, null)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(melees.get(club, meleeId)).hasStatus(HttpStatus.NOT_FOUND);
    }
}
