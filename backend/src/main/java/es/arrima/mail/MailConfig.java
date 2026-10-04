package es.arrima.mail;

import es.arrima.shared.http.OutboundHttp;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MailProperties.class)
class MailConfig {

    /** A missing variable stops the start-up with its name, instead of failing on the first e-mail. */
    @Bean
    EmailSender emailSender(MailProperties properties) {
        requireSet(properties.appUrl(), "APP_URL");
        return switch (properties.provider()) {
            case "brevo" -> {
                requireSet(properties.brevoApiKey(), "BREVO_API_KEY");
                requireSet(properties.senderEmail(), "MAIL_SENDER_EMAIL");
                if (!properties.appUrl().startsWith("https://")) {
                    throw new IllegalStateException("APP_URL must start with https:// when MAIL_PROVIDER=brevo");
                }
                yield new BrevoEmailSender(OutboundHttp.restClient(), properties);
            }
            case "log" -> new LoggingEmailSender();
            default -> throw new IllegalStateException(
                    "MAIL_PROVIDER must be 'brevo' or 'log', not '" + properties.provider() + "'");
        };
    }

    private static void requireSet(String value, String variable) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(variable + " is required");
        }
    }
}
