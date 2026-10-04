package es.arrima.participant;

public enum ParticipantStatus {
    ACTIVE,
    /** Signed up but did not come ("baja"): kept on the list, never drawn into a team, not asked to pay. */
    WITHDRAWN
}
