package es.arrima.live;

import static es.arrima.TestClubs.json;
import static org.assertj.core.api.Assertions.assertThat;

import es.arrima.TestClubs;
import es.arrima.TestClubs.RegisteredClub;
import es.arrima.TestMelees;
import es.arrima.TestcontainersConfiguration;
import es.arrima.auth.SignupInvitationRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Server-Sent Events end to end, over a real HTTP connection: a spectator's stream receives
 * "changed" when the admin records something.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class LiveEventsIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private SignupInvitationRepository invitations;

    @Test
    void spectatorsAreToldAboutEveryChange() throws Exception {
        RegisteredClub club = new TestClubs(mvc, invitations).register("Club en directo");
        TestMelees melees = new TestMelees(mvc);
        long meleeId = melees.create(club, 2);
        String code = json(melees.get(club, meleeId), "$.publicCode");

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:%d/api/public/melees/%s/events".formatted(port, code)))
                .header("Accept", "text/event-stream")
                .build();
        HttpResponse<Stream<String>> response = client.send(request, HttpResponse.BodyHandlers.ofLines());
        BlockingQueue<String> lines = new LinkedBlockingQueue<>();
        Thread reader = Thread.ofVirtual().start(() -> response.body().forEach(lines::add));

        try {
            assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(type -> assertThat(type).startsWith("text/event-stream"));
            assertThat(nextEvent(lines)).isEqualTo("event:ready");

            melees.signUp(club, meleeId, 4);

            assertThat(nextEvent(lines)).isEqualTo("event:changed");
        } finally {
            reader.interrupt();
            client.close();
        }
    }

    private static String nextEvent(BlockingQueue<String> lines) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (System.nanoTime() < deadline) {
            String line = lines.poll(200, TimeUnit.MILLISECONDS);
            if (line != null && line.startsWith("event:")) {
                return line;
            }
        }
        return "no event within 10 seconds";
    }
}
