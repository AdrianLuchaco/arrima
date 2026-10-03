package es.arrima.participant;

/**
 * One row of the editable preview shown before importing a WhatsApp list. The flags only help the
 * admin decide; the frontend unticks flagged rows by default.
 *
 * @param alreadyRegistered someone with the same name is already on this melee's list
 */
public record ImportPreviewEntry(Integer listNumber, String name, boolean struckThrough, boolean beforeListStart,
        boolean alreadyRegistered) {
}
