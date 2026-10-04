package es.arrima.participant;

/** Whether someone has paid the entry fee, as the admin records it at the sign-up table. */
public enum PaymentStatus {
    /** Not asked yet: everybody starts like this, and so do late sign-ups. */
    UNMARKED,
    PAID,
    /** Did not pay: stays on the list (and in the history) but does not play. */
    UNPAID
}
