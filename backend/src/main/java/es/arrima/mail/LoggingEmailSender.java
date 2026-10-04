package es.arrima.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Local development only: writes the e-mail to the log instead of sending it, so the password
 * recovery can be tried without a Brevo account. Never in production: the log would contain the
 * links, and anyone reading it could use them.
 */
class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(Email email) {
        log.info("E-mail not sent (MAIL_PROVIDER=log). To: {}\nSubject: {}\n\n{}", email.to(), email.subject(), email.text());
    }
}
