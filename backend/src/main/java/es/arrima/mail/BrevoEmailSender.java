package es.arrima.mail;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * E-mail through Brevo's HTTP API (free plan: 300 e-mails a day). Not SMTP: Render's free plan
 * blocks the SMTP ports. The API key stays in the backend.
 */
class BrevoEmailSender implements EmailSender {

    static final String API_URL = "https://api.brevo.com/v3";

    private final RestClient client;
    private final Contact sender;

    BrevoEmailSender(RestClient.Builder builder, MailProperties properties) {
        this.client = builder
                .baseUrl(API_URL)
                .defaultHeader("api-key", properties.brevoApiKey())
                .build();
        this.sender = new Contact(properties.senderName(), properties.senderEmail());
    }

    @Override
    public void send(Email email) {
        client.post()
                .uri("/smtp/email")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SendRequest(sender, List.of(new Recipient(email.to())), email.subject(), email.text(), email.html()))
                .retrieve()
                .toBodilessEntity();
    }

    /** The body Brevo expects (https://developers.brevo.com/reference/sendtransacemail). */
    record SendRequest(Contact sender, List<Recipient> to, String subject, String textContent, String htmlContent) {
    }

    record Contact(String name, String email) {
    }

    record Recipient(String email) {
    }
}
