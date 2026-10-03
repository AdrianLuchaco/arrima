package es.arrima.club;

import static es.arrima.TestClubs.json;
import static org.assertj.core.api.Assertions.assertThat;

import es.arrima.IntegrationTest;
import es.arrima.TestClubs;
import es.arrima.TestClubs.RegisteredClub;
import es.arrima.auth.SignupInvitationRepository;
import es.arrima.files.TestImages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@IntegrationTest
class ClubProfileIntegrationTest {

    private static final String VALID_PROFILE = """
            {"name":"%s","courtCount":12,"roundsCount":4,"prizeCount":6,
             "scoring":{"pointingOut":0,"pointingBigCircle":1,"pointingSmallCircle":2,"pointingNearJack":4,
                        "pointingOnJack":6,"shootingMiss":0,"shootingHit":1,"shootingHitOut":3,"shootingCarreau":6}}""";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private SignupInvitationRepository invitations;

    private TestClubs clubs;

    @BeforeEach
    void setUp() {
        clubs = new TestClubs(mvc, invitations);
    }

    @Test
    void aNewClubStartsWithTheDefaultSettingsAndPoints() {
        RegisteredClub club = clubs.register("Club Nuevo");

        assertThat(mvc.get().uri("/api/club").header(HttpHeaders.AUTHORIZATION, club.bearer()))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {"name":"Club Nuevo","logoUrl":null,"courtCount":8,"roundsCount":3,"prizeCount":5,
                         "scoring":{"pointingOut":0,"pointingBigCircle":1,"pointingSmallCircle":2,"pointingNearJack":3,
                                    "pointingOnJack":5,"shootingMiss":0,"shootingHit":1,"shootingHitOut":2,
                                    "shootingCarreau":5}}""");
    }

    @Test
    void theAdminUpdatesTheProfileAndTheNameIsCleaned() {
        RegisteredClub club = clubs.register("Club");

        assertThat(updateProfile(club, VALID_PROFILE.formatted("  Club   de Petanca\\u200B Arrima ")))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("""
                        {"name":"Club de Petanca Arrima","courtCount":12,"roundsCount":4,"prizeCount":6,
                         "scoring":{"pointingNearJack":4,"pointingOnJack":6,"shootingCarreau":6}}""");
    }

    @Test
    void invalidValuesAreRejectedFieldByField() {
        RegisteredClub club = clubs.register("Club");
        String zeroCourts = VALID_PROFILE.formatted("Club").replace("\"courtCount\":12", "\"courtCount\":0");
        String invisibleName = VALID_PROFILE.formatted(" \\u200B ");

        assertThat(updateProfile(club, zeroCourts))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.fields.courtCount").isEqualTo("Min");
        assertThat(updateProfile(club, invisibleName))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.fields.name").isEqualTo("NotBlank");
    }

    @Test
    void eachClubOnlySeesAndChangesItsOwnProfile() {
        RegisteredClub clubA = clubs.register("Club A");
        RegisteredClub clubB = clubs.register("Club B");

        updateProfile(clubA, VALID_PROFILE.formatted("Club A renombrado"));

        assertThat(mvc.get().uri("/api/club").header(HttpHeaders.AUTHORIZATION, clubB.bearer()))
                .bodyJson().extractingPath("$.name").isEqualTo("Club B");
        assertThat(mvc.get().uri("/api/club").header(HttpHeaders.AUTHORIZATION, clubA.bearer()))
                .bodyJson().extractingPath("$.name").isEqualTo("Club A renombrado");
    }

    @Test
    void theProfileNeedsAuthentication() {
        assertThat(mvc.get().uri("/api/club"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.code").isEqualTo("UNAUTHENTICATED");
    }

    @Test
    void anUploadedLogoIsServedThroughItsSignedLink() {
        RegisteredClub club = clubs.register("Club con logo");
        byte[] png = TestImages.png(64, 64);

        MvcTestResult upload = uploadLogo(club, new MockMultipartFile("file", "logo.png", "image/png", png));
        assertThat(upload).hasStatusOk();
        String logoUrl = json(upload, "$.logoUrl");

        MvcTestResult image = mvc.get().uri(logoUrl).exchange();
        assertThat(image).hasStatusOk().hasContentType(MediaType.IMAGE_PNG);
        assertThat(image.getResponse().getContentAsByteArray()).isEqualTo(png);
    }

    @Test
    void aFileThatIsNotAnImageIsRejectedWhateverItsName() {
        RegisteredClub club = clubs.register("Club");
        MockMultipartFile fake = new MockMultipartFile("file", "logo.png", "image/png", "<svg onload=alert(1)>".getBytes());

        assertThat(uploadLogo(club, fake))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.code").isEqualTo("INVALID_IMAGE");
    }

    @Test
    void aTamperedFileLinkIsRejected() {
        RegisteredClub club = clubs.register("Club");
        String logoUrl = json(uploadLogo(club, new MockMultipartFile("file", "logo.png", "image/png",
                TestImages.png(8, 8))), "$.logoUrl");
        String otherFile = logoUrl.replaceFirst("logo-[a-f0-9-]+", "logo-00000000-0000-0000-0000-000000000000");

        assertThat(mvc.get().uri(otherFile))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.code").isEqualTo("INVALID_FILE_LINK");
    }

    @Test
    void theLogoCanBeRemoved() {
        RegisteredClub club = clubs.register("Club");
        uploadLogo(club, new MockMultipartFile("file", "logo.jpg", "image/jpeg", TestImages.jpeg(8, 8)));

        assertThat(mvc.delete().uri("/api/club/logo").header(HttpHeaders.AUTHORIZATION, club.bearer()))
                .hasStatusOk()
                .bodyJson().extractingPath("$.logoUrl").isNull();
    }

    private MvcTestResult updateProfile(RegisteredClub club, String body) {
        return mvc.put().uri("/api/club")
                .header(HttpHeaders.AUTHORIZATION, club.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    private MvcTestResult uploadLogo(RegisteredClub club, MockMultipartFile file) {
        return mvc.post().uri("/api/club/logo")
                .header(HttpHeaders.AUTHORIZATION, club.bearer())
                .multipart()
                .file(file)
                .exchange();
    }
}
