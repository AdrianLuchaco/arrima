package es.arrima.participant;

public enum ParticipantStatus {
    ACTIVE,
    /** Signed up but did not come ("anulado"): kept and shown in red, never drawn into a team. */
    WITHDRAWN
}
