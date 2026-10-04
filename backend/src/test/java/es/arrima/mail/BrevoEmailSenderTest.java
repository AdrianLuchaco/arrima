package es.arrima.mail;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** The exact request Brevo receives: we cannot call the real API from the tests. */
class BrevoEmailSenderTest {

    @Test
    void sendsTheEmailToBrevosApiWithTheKeyInItsHeader() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer brevo = MockRestServiceServer.bindTo(builder).build();
        MailProperties properties = new MailProperties("brevo", "test-api-key", "club@example.com", "Arrima", "https://arrima.test");
        BrevoEmailSender sender = new BrevoEmailSender(builder, properties);

        brevo.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "test-api-key"))
                .andExpect(content().json("""
                        {
                          "sender": {"name": "Arrima", "email": "club@example.com"},
                          "to": [{"email": "admin@example.com"}],
                          "subject": "Asunto",
                          "textContent": "Texto",
                          "htmlContent": "<p>Texto</p>"
                        }"""))
                .andRespond(withStatus(HttpStatus.CREATED));

        sender.send(new Email("admin@example.com", "Asunto", "Texto", "<p>Texto</p>"));

        brevo.verify();
    }
}
