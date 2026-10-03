package es.arrima.melee;

/** Something in a melee changed. Live viewers are notified once the transaction commits. */
public record MeleeChangedEvent(long meleeId, String publicCode) {
}
