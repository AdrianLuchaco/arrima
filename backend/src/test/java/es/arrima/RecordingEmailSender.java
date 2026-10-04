package es.arrima;

import es.arrima.mail.Email;
import es.arrima.mail.EmailSender;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/** Keeps the e-mails instead of sending them, so tests can read them. */
public class RecordingEmailSender implements EmailSender {

    private final BlockingQueue<Email> sent = new LinkedBlockingQueue<>();

    @Override
    public void send(Email email) {
        sent.add(email);
    }

    /**
     * E-mails go out in the background after the commit: this waits for the next one to the given
     * address. Others (late ones from an earlier test) are skipped.
     */
    public Email awaitNextTo(String to) {
        Email email = nextTo(to, Duration.ofSeconds(5));
        if (email == null) {
            throw new AssertionError("No e-mail was sent to " + to);
        }
        return email;
    }

    /** True if nothing reaches the address within the given time (enough for the background sending). */
    public boolean sendsNothingTo(String to, Duration wait) {
        return nextTo(to, wait) == null;
    }

    private Email nextTo(String to, Duration wait) {
        long deadline = System.nanoTime() + wait.toNanos();
        try {
            while (true) {
                Email email = sent.poll(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                if (email == null || email.to().equals(to)) {
                    return email;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    public void clear() {
        sent.clear();
    }
}
