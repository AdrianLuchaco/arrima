package es.arrima.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param provider    "brevo" in production, "log" for local development (see LoggingEmailSender)
 * @param brevoApiKey Brevo API key (xkeysib-...): it stays in the backend
 * @param senderEmail sender address, verified in Brevo
 * @param senderName  name shown as the sender
 * @param appUrl      public address of the frontend, for the links inside the e-mails. Taken from
 *                    the configuration, never from the request: a forged Host header must not be
 *                    able to send recovery links pointing to someone else's site.
 */
@ConfigurationProperties("arrima.mail")
public record MailProperties(
        String provider,
        String brevoApiKey,
        String senderEmail,
        String senderName,
        String appUrl) {
}
