package es.arrima.participant;

/** A person as typed or confirmed by the admin (name still unchecked). */
public record NewParticipant(Integer listNumber, String name) {
}
