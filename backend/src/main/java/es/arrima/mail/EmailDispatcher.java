package es.arrima.mail;

import jakarta.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Sends e-mails after the transaction commits, in the background:
 * <ul>
 *   <li>after the commit, so no e-mail goes out for a change that was rolled back;</li>
 *   <li>in the background, so the response does not wait for Brevo. That also keeps the password
 *       recovery from revealing which e-mails have an account: with and without an e-mail to send,
 *       the answer takes the same time.</li>
 * </ul>
 * A failed e-mail is logged (without its content, which may hold a link) and not retried: the
 * admin can ask for another one.
 */
@Component
public class EmailDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatcher.class);

    private final EmailSender sender;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    EmailDispatcher(EmailSender sender) {
        this.sender = sender;
    }

    public void sendAfterCommit(Email email) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            executor.execute(() -> sendQuietly(email));
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                executor.execute(() -> sendQuietly(email));
            }
        });
    }

    /** On shutdown, e-mails already handed over are still sent. */
    @PreDestroy
    void finishPending() {
        executor.close();
    }

    private void sendQuietly(Email email) {
        try {
            sender.send(email);
        } catch (RuntimeException e) {
            log.error("Could not send the e-mail \"{}\": {}", email.subject(), e.getMessage());
        }
    }
}
