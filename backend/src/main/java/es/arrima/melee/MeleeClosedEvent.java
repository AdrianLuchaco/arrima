package es.arrima.melee;

/** The melee was closed (by the admin or after 20 idle minutes): it is history now. */
public record MeleeClosedEvent(long meleeId) {
}
