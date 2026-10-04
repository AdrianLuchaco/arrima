package es.arrima.mail;

/**
 * An e-mail ready to send, in plain text and in HTML (mail apps show the one they prefer).
 * Whoever builds it must escape any user text that goes into the HTML.
 */
public record Email(String to, String subject, String text, String html) {
}
