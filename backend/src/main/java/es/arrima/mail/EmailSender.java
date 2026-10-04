package es.arrima.mail;

/** Sends one e-mail now. Use EmailDispatcher, which sends after the commit and off the request. */
public interface EmailSender {

    void send(Email email);
}
